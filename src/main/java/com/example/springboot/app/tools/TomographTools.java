package com.example.springboot.app.tools;

import com.example.springboot.app.MainPageController;
import org.springframework.util.MultiValueMap;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TomographTools {

    final static double pi = Math.PI;
    final static double sq2 = Math.sqrt(2.0);
    final static double piPow = Math.PI * Math.PI;
    final static int travelAngle = 180;

    public static void processAndAddImageToResponse(int c, int outputW, int outputH, char[][] chars, MultiValueMap<String, Object> body) throws IOException {
        int[][] img = new int[outputH][outputW];
        for(int i=0; i<outputH; i++){
            for(int j=0; j<outputW; j++){
                img[i][j] = ImageTools.grayscaleToRgb(chars[i][j]);
            }
        }
        String image = ImageTools.rawToBase64("png", img, BufferedImage.TYPE_INT_RGB);
        //String image = ImageTools.rawToBase64("png", img, BufferedImage.TYPE_USHORT_GRAY);
        synchronized (MainPageController.class) {
            body.add("img"+c, image);
        }
        img = null;
        image = null;
        System.gc();
    }
    public static String processWithoutAddingImage(int c, int outputW, int outputH, char[][] chars) throws IOException {
        int[][] img = new int[outputH][outputW];
        for(int i=0; i<outputH; i++){
            for(int j=0; j<outputW; j++){
                img[i][j] = ImageTools.grayscaleToRgb(chars[i][j]);
            }
        }
        return ImageTools.rawToBase64("png", img, BufferedImage.TYPE_INT_RGB);
    }

    public static double[] generateRampKernel (int size){
        double brightness = 1;
        if (size%2 == 0) {size++;} // size must be ODD, make it if its not
        int centerPivot = size/2;

        double[] kernel = new double[size];

        kernel[centerPivot] = 1 * brightness; // centerElement = 1
        int distanceFromCenter = 0; // first element next to center is ODD indexed

        for (int i=centerPivot+1; i<size; i++){
            distanceFromCenter++; // first increase distance by 1
            int mirrorElemIndex = i - (distanceFromCenter*2);
            if (distanceFromCenter%2 == 0) { // elements distant from center by EVEN number
                kernel[i] = 0;
            } else { // elements distant from center by ODD number
                kernel[i] = ( (-4 * brightness) /piPow)/(distanceFromCenter*distanceFromCenter);
            }
            kernel[mirrorElemIndex] = kernel[i]; // element on the opposite site of center has the same value
        }
        return kernel;
    }

    public static double[][] convolveSame(double[][] input, double[] kernel){
        double[][] output = new double[input.length][input[0].length];
        for (int i=0; i<input.length; i++) {
            System.arraycopy(convolveSame(input[i], kernel), 0, output[i], 0, input[i].length);
        }
        return output;
    }
    public static double[] convolveSame(double[] input, double[] kernel){

        double[] output = new double[input.length];

        int kernelCenter = kernel.length/2;
        for (int i=0; i<input.length; i++){ // for all input numbers

            //set at which INPUT INDEX, kernel begins and ends - move kernel so kernels center is at current input index
            int kernelBegin = i-kernelCenter;
            int kernelEnd = kernelBegin + kernel.length;
            int currKernBegin = Math.max(kernelBegin, 0); // actual starting point of this loop - cut off invalid numbers (index > 0)
            int currKernEnd = Math.min(kernelEnd, input.length); // actual ending point of this loop - cut off invalid numbers (index out of range for input.length)

            int kernelIndexPointer = currKernBegin-kernelBegin; // where does currKernBegin is as KERNEL INDEX

            double localSum = 0;
            for (int k=currKernBegin; k<currKernEnd; k++){ // then for all kernel numbers within valid range
                // K stands for index in input[] and its increasing
                // kernelIndexPointer in index in kernel[] and its also increasing
                // multiply input with its respective kernel
                // sum up
                localSum += input[k]*kernel[kernelIndexPointer++];
            }
            output[i] = localSum;
        }

        return output;
    }
    public static double[][] convolveSame(char[][] input, double[] kernel){
        double[][] output = new double[input.length][input[0].length];
        for (int i=0; i<input.length; i++) {
            System.arraycopy(convolveSame(input[i], kernel), 0, output[i], 0, input[i].length);
        }
        return output;
    }
    public static double[] convolveSame(char[] input, double[] kernel){

        double[] output = new double[input.length];

        int kernelCenter = kernel.length/2;
        for (int i=0; i<input.length; i++){ // for all input numbers

            //set at which INPUT INDEX, kernel begins and ends - move kernel so kernels center is at current input index
            int kernelBegin = i-kernelCenter;
            int kernelEnd = kernelBegin + kernel.length;
            int currKernBegin = Math.max(kernelBegin, 0); // actual starting point of this loop - cut off invalid numbers (index > 0)
            int currKernEnd = Math.min(kernelEnd, input.length); // actual ending point of this loop - cut off invalid numbers (index out of range for input.length)

            int kernelIndexPointer = currKernBegin-kernelBegin; // where does currKernBegin is as KERNEL INDEX

            double localSum = 0;
            for (int k=currKernBegin; k<currKernEnd; k++){ // then for all kernel numbers within valid range
                // K stands for index in input[] and its increasing
                // kernelIndexPointer in index in kernel[] and its also increasing
                // multiply input with its respective kernel
                // sum up
                localSum += input[k]*kernel[kernelIndexPointer++];
            }
            output[i] = localSum;
        }

        return output;
    }

    public static double getBrightness(int w, int h, double inputAvg, List<int[]>[][] depencies, double[][] sinogram){

        int[][] canvas = new int[h][w]; // make new canvas

        for (int r=0; r<sinogram.length; r++) { // for every rotation
            for (int s = 0; s<sinogram[0].length; s++) { // for each sensor
                //brighten up all the related pixels
                List<int[]> pixelList = depencies[r][s];
                for (int[] pix : pixelList){
                    if(pix[0] >= 0){
                        canvas[pix[1]][pix[0]] += sinogram[r][s];
                    }
                }
            }
        }

        // auto adjust
        char[][] partialOut = new char[1][1];
        double error = 100;
        double topBound = 10, bottomBound = 0;
        double localBrightness = 5;
        int counter = 0;

        while( error > 2 ) {
            partialOut = normalize(canvas, localBrightness);
            double avg = getAvg(partialOut);
            error = Math.abs(inputAvg - avg);
            if(inputAvg > avg && error > 10) {
                bottomBound = localBrightness;
                localBrightness = (localBrightness + topBound) /2;
            } else if (error > 10) {
                topBound = localBrightness;
                localBrightness = (localBrightness + bottomBound) /2;
            } else if (counter++ > 10){
                break;
            }
        }
        return localBrightness;
    }

    public static Stream<char[][]> reconstructImage(int w, int h, double brightness, List<int[]>[][] depencies, double[][] sinogram){

        int[][] canvas = new int[h][w]; // make new canvas
        int[] arr = IntStream.range(0, sinogram.length).toArray();

        return Arrays.stream(arr).mapToObj(r -> {

            for (int s = 0; s<sinogram[0].length; s++) { // for each sensor
                //brighten up all the related pixels
                List<int[]> pixelList = depencies[r][s];
                for (int[] pix : pixelList){
                    if(pix[0] >= 0){
                        canvas[pix[1]][pix[0]] += sinogram[r][s];
                    }
                }
            }

            char[][] partialOut = normalize(canvas, brightness);

            return Arrays.stream(partialOut)
                    .map(char[]::clone)
                    .toArray(char[][]::new);

        }).parallel();
    }

    private static char[][] normalize(int[][] canvas, double brightness){

        char[][] out = new char[canvas.length][canvas[0].length];

        //find max canvas value to normalize outcome
        int maxVal = getMax(canvas);

        //get ratio of the brightest pixel to max value (255)
        double ratio = (255 * brightness)/(double) maxVal ;
        // divide every pixel by this and cap at 255
        for (int i=0; i<canvas.length; i++) { // for every canvas pixel
            for (int j=0; j<canvas[0].length; j++) {
                // max it at 255 and save in output
                out[i][j] = (char)Math.min(255, Math.max(0, canvas[i][j]*ratio ));
            }
        }
        return out;
    }

    public static double getAvg(char[][] pic) {
        int[] sumArray = new int[pic.length];
        for (int i = 0; i<pic.length; i++) { // for every canvas pixel
            for (int j = 0; j < pic[0].length; j++) {
                //each row in separate counter - to avoid overflow
                sumArray[i] += pic[i][j];
            }
        }

        int rowLen = pic[0].length;
        return Arrays.stream(sumArray).mapToDouble(i ->  (double)i/rowLen ).sum() / pic.length;
    }

    public static int getMax(int[][] pic) {
        int maxVal = 0;
        for (int i=0; i<pic.length; i++) { // for every canvas pixel
            for (int j = 0; j < pic[0].length; j++) {
                // save if max
                if (pic[i][j] > maxVal) {
                    maxVal = pic[i][j];
                }
            }
        }
        return maxVal;
    }

    public static List<int[]>[][] bresenhamForAll(int w, int h, double angleStep, int sensorNumber, double sensorSpread) {

        int halfW = w/2, halfH = h/2;
        double d = Math.sqrt( (w*w) + (h*h) );
        double r = d / 2;

//        int angleStep = 10;
//        int sensorNumber = 100;
//        int sensorSpread = 60;

        int stepCount = (int)(travelAngle/angleStep);

        double rotationStep = (double)travelAngle/stepCount;
        double sensorSpreadRadian = Math.toRadians(sensorSpread);
        double sensorStepRadian = sensorSpreadRadian/sensorNumber;

        @SuppressWarnings("unchecked")
        List<int[]>[][] outArray = new List[stepCount][sensorNumber];

        for (int i=0; i<stepCount; i++) { // for every rotational step

            double currAngle = i * rotationStep; // set angle of central sensor
            double currAngleRadian = Math.toRadians(currAngle);
            double minAngleRadian = currAngleRadian - (sensorSpreadRadian / 2); // angle of most left sensor
            double a = 0; // assign value below, y=ax+b
            if (currAngle != 90.0) {
                a = Math.tan(currAngleRadian);
            }

            int[][][] positions = new int[sensorNumber][2][2]; // array for each sensor position and opposite side of where they aim

            for (int s = 0; s < sensorNumber; s++) { // for each sensor

                double angle = minAngleRadian + (s * sensorStepRadian); //sensors actual angle based on minAngleRadian

                positions[s][0][0] = (int) Math.max((r * Math.cos(angle)) + halfW, 0); // x of sensor
                positions[s][0][1] = (int) Math.max((r * Math.sin(angle)) + halfH, 0); // y of sensor
                positions[(sensorNumber - 1) - s][1][0] = (int) Math.min((r * Math.cos(angle + pi)) + halfW, w); // x of sensor on the opposite side
                positions[(sensorNumber - 1) - s][1][1] = (int) Math.min((r * Math.sin(angle + pi)) + halfH, h); // y of sensor on the opposite side
            }

            for (int s = 0; s < sensorNumber; s++) { // for each sensor AGAIN - now that every sensor has its start and end-points
                // get all pixel between starting point and end point
                // filter those out of image size and mark them as {-1, -1}
                List<int[]> line = findLine(positions[s][0][0], positions[s][0][1], positions[s][1][0], positions[s][1][1]);
                line = line.stream()
                        .peek(pix -> {
                            if (pix[0] < 0 || pix[0] >= w || pix[1] < 0 || pix[1] >= h) {
                                pix[0] = -1;
                                pix[1] = -1;
                            }
                        })
                        .collect(Collectors.toList());

                //add to outcome
                outArray[i][s] = line;
            }
        }
        return outArray;
    }

    public static char[][] generateSinogram(double angleStep, int sensorNumber, List<int[]>[][] depencies, char[][] pic) {

        int stepCount = (int)(travelAngle/angleStep);

        char[][] returnMatrix = new char[stepCount][sensorNumber];

        for (int i=0; i<stepCount; i++) { // for every rotational step
            for (int s=0; s<sensorNumber; s++) { // for each sensor

                // sum list values and get final brightness of a sensor; if pixel is out of image, add 0 (it is black)
                int sum=0, count=0;
                for (int[] pix : depencies[i][s]) {
                    if(pix[0] >= 0) {
                        sum += pic[pix[1]][pix[0]];
                    }
                    count += 1;
                }

                // return average brightness, but clip it to 0-255 and cast to char
                returnMatrix[i][s] = (count!=0) ? (char)Math.max( Math.min( (sum/count) , 255 ) , 0 ) : 0;

            }
        }

        return returnMatrix;
    }

    /**
     * source: https://www.sanfoundry.com/java-program-bresenham-line-algorithm/
     **/
    public static List<int[]> findLine(int x0, int y0, int x1, int y1)
    {
        List<int[]> line = new ArrayList<>();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx-dy;
        int e2;

        while (true)
        {
            line.add(new int[] {x0, y0});

            if (x0 == x1 && y0 == y1)
                break;

            e2 = 2 * err;
            if (e2 > -dy)
            {
                err = err - dy;
                x0 = x0 + sx;
            }

            if (e2 < dx)
            {
                err = err + dx;
                y0 = y0 + sy;
            }
        }
        return line;
    }

    public static double getRSM(boolean dynamicResolution, char[][] arrIn, char[][] arrOut){
        double RSM = 0;
        int iw = arrIn[0].length, ih = arrIn.length, ow = arrOut[0].length, oh = arrOut.length;
        if(dynamicResolution){ // if different resolutions
            // flags to get shorter dimensions; 1 = INPUT, 2 = OUTPUT;

            for (int i=0; i< Math.min(ih, oh); i++){
                for (int j=0; j< Math.min(iw, ow); j++){
                    int inW = (iw < ow) ? j : getPixelAt(ow, iw, j) ;
                    int inH = (ih < oh) ? i : getPixelAt(oh, ih, i) ;
                    int outW = (iw < ow) ? getPixelAt(oh, ih, j) : j ;
                    int outH = (ih < oh) ? getPixelAt(ih, oh, i) : i ;
                    double holder = arrIn[inH][inW] - arrOut[outH][outW];
                    holder = holder * holder;
                    RSM += (holder>=0) ? holder : holder*-1;
                }
            }

        } else { // if resolution is the same as input
            for (int i=0; i<ih; i++) {
                for (int j=0; j<iw; j++) {
                    double holder = arrIn[i][j] - arrOut[i][j];
                    holder = holder * holder;
                    RSM += (holder>=0) ? holder : holder*-1;
                }
            }

        }
        return Math.sqrt(RSM/(Math.min(iw, ow)*Math.min(ih, oh)));
    }

    private static int getPixelAt(int inSize, int outSize, int inIndex){
        return Math.round((float)(inIndex/inSize) * outSize);
    }

}
