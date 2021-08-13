package com.example.springboot.app;

import com.example.springboot.app.tools.ImageTools;
import com.example.springboot.app.tools.TomographTools;

import org.dcm4che2.data.*;

import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che3.util.UIDUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@Controller
@CrossOrigin(origins="http://localhost")
public class MainPageController {

    @RequestMapping(value="/tomograph", method=PUT, produces="multipart/form-data")
    @ResponseBody
    public ResponseEntity<MultiValueMap<String, Object>> tomograph(@RequestParam String alphaStep,
                        @RequestParam int count,
                        @RequestParam String spread,
                        @RequestParam String brightness,
                        @RequestParam boolean autoBrightness,
                        @RequestParam MultipartFile picture,
                        @RequestParam boolean filter,
                        @RequestParam boolean dynamicResolution,
                        @RequestParam boolean noSlider,
                        HttpServletResponse response) {

        System.out.println("\nConnected to Tomograph Simulator");

        String filename = picture.getOriginalFilename();
        assert filename != null;
        boolean isDICOM = filename.substring(filename.length() - 4).equals(".dcm");

        double spreadD = Double.parseDouble(spread);
        double alphaStepD = Double.parseDouble(alphaStep);
        double brightnessD = autoBrightness ? -1 : Double.parseDouble(brightness);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        try {
            //load
            BufferedImage inImg;
            int w, h;

            if(isDICOM){ // if it is a DICOM file
                File dicomFile = new File("placeholder");
                try (OutputStream os = new FileOutputStream(dicomFile)) {
                    os.write(picture.getBytes());
                }
                ImageIO.scanForPlugins();
                ImageReader reader = (ImageReader) ImageIO.getImageReadersByFormatName("DICOM").next();
                ImageInputStream iis = ImageIO.createImageInputStream(dicomFile);
                reader.setInput(iis, false);
                inImg = reader.read(0);

            } else { // if its not a DICOM file
                inImg = ImageIO.read(picture.getInputStream());
            }

            w = inImg.getWidth();
            h = inImg.getHeight();

            int[] rawInt = inImg.getRGB(0, 0, w, h, null, 0, w);
            int[][] rawIntCanvas = ImageTools.toInt2D(rawInt, w, h);

            System.out.println("Generating grayscale...");
            // process data
            char[][] grayscale = new char[h][w];

            for(int i=0; i<h; i++){
                for (int j=0; j<w; j++){
                    grayscale[i][j] = ImageTools.rgbToGrayscaleChar(rawIntCanvas[i][j]);
                }
            }

            // make sure to free memory!
            rawInt = null;
            System.gc();


            System.out.println("Creating Sinogram...");
            //actual sinogram
            List<int[]>[][] depencies = TomographTools.bresenhamForAll(w, h, alphaStepD, count, spreadD); // list of pixels affected for every sensor on every scan
            char[][] sinogram = TomographTools.generateSinogram(alphaStepD, count, depencies, grayscale);

            System.out.println("Normalizing Sinogram...");
            // sinogram for display
            char max = 0;
            for (int i=0; i<sinogram.length; i++){
                for (int j=0; j<sinogram[0].length; j++){
                    if(sinogram[i][j] > max){max = sinogram[i][j];};
                }
            }
            double ratio = 255/(double)max;
            char[][] displaySinogram = new char[sinogram.length][sinogram[0].length];
            for(int i=0; i<sinogram.length; i++) {
                for (int j=0; j<sinogram[i].length; j++){
                    displaySinogram[i][j] = (char) Math.min(255, (char)(sinogram[i][j]*ratio) );
                }
            }

            int sinH = sinogram.length, sinW = sinogram[0].length;

            //sinogram filtering
            double[][] rampFilteredSinogram;
            if(filter){
                System.out.println("Convolving Sinogram...");
                rampFilteredSinogram = TomographTools.convolveSame(displaySinogram, TomographTools.generateRampKernel(21));
            } else {
                rampFilteredSinogram = Arrays.stream(displaySinogram)
                        .map(
                            a -> IntStream.range(0, a.length)
                            .mapToDouble(i -> a[i])
                            .toArray()
                        )
                        .toArray(double[][]::new);
            }

            // make sure to free memory!
            sinogram = null;
            System.gc();

            System.out.println("Image Reconstructions (multiple pictures)...");
            // image reconstruction
            int outputW, outputH;
            if (dynamicResolution){
                boolean longerSide = w > h;
                double whRatio = (double) w/h;
                outputW = longerSide ? (int)(count*whRatio) : count;
                outputH = longerSide ? count : (int)(count/whRatio);
                depencies = TomographTools.bresenhamForAll(outputW, outputH, alphaStepD, count, spreadD);
            } else {
                outputW = w;
                outputH = h;
            }

            if(autoBrightness){
                System.out.println("Calculating brightness...");
                double inputAvg = TomographTools.getAvg(grayscale);
                brightnessD = TomographTools.getBrightness(outputW, outputH, inputAvg, depencies, rampFilteredSinogram);
            }

            Stream<char[][]> output = TomographTools.reconstructImage(outputW, outputH, brightnessD, depencies, rampFilteredSinogram);

            //results... return processed data as picture
            int[][] out0 = new int[sinH][sinW];

            ImageTools.forEachPixel(sinH, sinW, (i, j) -> {
                out0[i][j] = ImageTools.grayscaleToRgb(displaySinogram[i][j]);
            });


//            ImageTools.forEachPixel(outputH, outputW, (i, j) -> {
//                out1[i][j] = ImageTools.grayscaleToRgb(output.get(output.size()-1)[i][j]);
//            });
//
            String inputImage = ImageTools.rawToBase64("png", rawIntCanvas, BufferedImage.TYPE_INT_RGB);
            String processedImage = ImageTools.rawToBase64("png", out0, BufferedImage.TYPE_INT_RGB);

            MultiValueMap<String, Object> body
                    = new LinkedMultiValueMap<>();
            body.add("file1", inputImage);
            body.add("file2", processedImage);

            System.out.println("Converting outputImages...");
            AtomicInteger c = new AtomicInteger(); // this is so it can be increased inside lambda in Stream.map - stream size

            char[][] lastImage;
            if(!noSlider){ // if want everything
                lastImage = output.peek(chars -> {
                    try {
                        int holder = 0;
                        synchronized (this) {
                            holder = c.get();
                            c.set(holder + 1);
                            System.out.print(holder+" out of "+rampFilteredSinogram.length+"\r");
                        }
                        String str = TomographTools.processWithoutAddingImage(holder, outputW, outputH, chars);
                        synchronized (MainPageController.class) {
                            body.add("img"+holder, str);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }).skip(sinH - 1).findFirst().orElse(null);
            } else { // else if user only wants outcome
                lastImage = output.skip(sinH - 1).findFirst().orElse(null);
            }

            System.out.println("Converting output to RGB...");
            if (lastImage != null){ // if there is a last image
                int[][] out1 = new int[lastImage.length][lastImage[0].length];
                ImageTools.forEachPixel(outputH, outputW, (i, j) -> {
                    out1[i][j] = ImageTools.grayscaleToRgb(lastImage[i][j]);
                });
                String outputImage = ImageTools.rawToBase64("png", out1, BufferedImage.TYPE_INT_RGB);
                body.add("file3", outputImage);
            }

            body.add("imgCount", c); //add pic count for slideshow

            System.out.println("Calculating RMS...");
            // calculate error
            assert lastImage != null;
            double quality = TomographTools.getRSM(dynamicResolution, grayscale, lastImage);

            body.add("quality", Double.toString(quality));

            System.out.println("Done!");

            return new ResponseEntity<>(
                    body,
                    headers,
                    HttpStatus.CREATED
            );

        } catch (IOException e) {
            e.printStackTrace();
        }

        MultiValueMap<String, Object> body
                = new LinkedMultiValueMap<>();
        body.add("file1", "");

        System.out.println("I/O fail...");
        
        return new ResponseEntity<>(
                body,
                headers,
                HttpStatus.CREATED
        );
    }

    @RequestMapping(value="/dicomGenerator", method=PUT, produces="multipart/form-data" /*produces="application/octet-stream"*/)
    @ResponseBody
    public ResponseEntity<byte[]> dicomGenerator(@RequestParam(required = false) MultipartFile picture,
                                                 HttpServletResponse response) throws IOException {

        System.out.println("\nConnected to DICOM Generator");

        String filename = picture.getOriginalFilename();
        assert filename != null;
        boolean isDICOM = filename.substring(filename.length() - 4).equals(".dcm");

        //load
        BufferedImage inImg;
        int w, h;
        File dicomFile = new File("out.dcm");

        if(isDICOM){ // if it is a DICOM file
//                DicomInputStream din = new DicomInputStream(picture.getInputStream());
//                DicomObject dcmObj = din.readDicomObject();
//                w = dcmObj.getInt(Tag.Columns);
//                h = dcmObj.getInt(Tag.Rows);

            try (OutputStream os = new FileOutputStream(dicomFile)) {
                os.write(picture.getBytes());
            }
            ImageIO.scanForPlugins();
            ImageReader reader = (ImageReader) ImageIO.getImageReadersByFormatName("DICOM").next();
            ImageInputStream iis = ImageIO.createImageInputStream(dicomFile);
            reader.setInput(iis, false);
            inImg = reader.read(0);

        } else { // if its not a DICOM file
            inImg = ImageIO.read(picture.getInputStream());
        }

        w = inImg.getWidth();
        h = inImg.getHeight();
        int[] rawInt = inImg.getRGB(0, 0, w, h, null, 0, w);
        byte[] grayscale = new byte[h*w];


        for(int i=0; i<h*w; i++){
            grayscale[i] = ImageTools.rgbToGrayscaleByte(rawInt[i]);
        }

        int colorComponents = inImg.getColorModel().getNumColorComponents();
        int bitsPerPixel = inImg.getColorModel().getPixelSize();
        int bitsAllocated = (bitsPerPixel / colorComponents);

        DicomObject dicom = new BasicDicomObject();

        // DICOM headers - image related
        // headers
       // dicom.putString(Tag.ImageType, VR.CS, "1");
        dicom.putString(Tag.SpecificCharacterSet, VR.CS, "ISO_IR 100");
        dicom.putString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
        dicom.putInt(Tag.SamplesPerPixel, VR.US, 1);
        dicom.putInt(Tag.BitsAllocated, VR.US, 8);
        dicom.putInt(Tag.BitsStored, VR.US, 8);
        dicom.putInt(Tag.HighBit, VR.US, 8-1);
        // image data description
        dicom.putInt(Tag.Rows, VR.US, h);
        dicom.putInt(Tag.Columns, VR.US, w);
        dicom.putInt(Tag.PixelRepresentation, VR.US, 0);
        //pixels
        dicom.putBytes(Tag.PixelData, VR.OW, grayscale);
        // date of creation
        dicom.putDate(Tag.InstanceCreationDate, VR.DA, new Date());
        dicom.putDate(Tag.InstanceCreationTime, VR.TM, new Date());
        dicom.putString(Tag.StudyInstanceUID, VR.UI, UIDUtils.createUID());
        dicom.putString(Tag.SeriesInstanceUID, VR.UI, UIDUtils.createUID());
        dicom.putString(Tag.SOPClassUID, VR.UI, UIDUtils.createUID());
        dicom.putString(Tag.SOPInstanceUID, VR.UI, UIDUtils.createUID());
        // patient detail
        dicom.putString(Tag.Modality, VR.CS, "CT");
        dicom.putString(Tag.PersonName, VR.PN, "Test^Name");
        dicom.putString(Tag.PatientID, VR.LO, "1");

        // meta info
        dicom.initFileMetaInformation("1.2.840.10008.1.2 ");

        // write to file
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        DicomOutputStream dos = new DicomOutputStream(bos);
        dos.writeDicomFile(dicom);
        dos.writeHeader(Tag.PixelData, VR.OB, -1);


        byte[] body = fos.toByteArray();
        dos.close();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        System.out.println("Done!");

        return new ResponseEntity<>(
                body,
                headers,
                HttpStatus.CREATED
        );
    }

}
