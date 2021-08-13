package com.example.springboot.app.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.MemoryImageSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

public class ImageTools {

    public static String rawToBase64(String type, int width, int height, byte[][][] data, int targetType) throws IOException {
        int[] inBetween = toInt1D(data);
        byte[] datax = compressImage(type, width, height, inBetween, targetType);
        return Base64.getEncoder().encodeToString(datax);
    }
    public static String rawToBase64(String type, int width, int height, byte[] data, int targetType) throws IOException {
        int[] inBetween = toInts(data);
        byte[] datax = compressImage(type, width, height, inBetween, targetType);
        return Base64.getEncoder().encodeToString(datax);
    }
    public static String rawToBase64(String type, int[][] data, int targetType) throws IOException {
        int width = data[0].length,  height = data.length;
        int[] flatData = Stream.of(data).flatMapToInt(Arrays::stream).toArray();
        byte[] datax = compressImage(type, width, height, flatData, targetType);
        return Base64.getEncoder().encodeToString(datax);
    }
    public static String rawToBase64(String type, char[][] data, int targetType) throws IOException {
        int width = data[0].length,  height = data.length;
        int[] flatData = Stream.of(data).flatMapToInt(x -> CharBuffer.wrap(x).chars()).toArray();
        byte[] datax = compressImage(type, width, height, flatData, targetType);
        return Base64.getEncoder().encodeToString(datax);
    }
    public static String rawToBase64(String type, int width, int height, int[] data, int targetType) throws IOException {
        byte[] datax = compressImage(type, width, height, data, targetType);
        return Base64.getEncoder().encodeToString(datax);
    }



    public static byte[][][] toByte3D(int[] arr, int w, int h){
        byte[][][] out = new byte[h][w][4];
        forEachPixel(h, w, (i, j) -> out[i][j] = toBytes(arr[(i*w)+j]) );
        return out;
    }
    public static byte[][] toBytes(int[] inputArray){
        int size = inputArray.length;
        byte[][] out = new byte[size][4];
        forEachPixel(size, i -> out[i] = toBytes(inputArray[i]) );
        return out;
    }
    public static byte[] toBytes(int inputNumber){
        byte b0 = (byte) (inputNumber >> 24);
        byte b1 = (byte) (inputNumber >> 16);
        byte b2 = (byte) (inputNumber >> 8);
        byte b3 = (byte) (inputNumber);
        return new byte[] {b0, b1, b2, b3};
    }
    public static char[][][] toChar3D(int[] arr, int w, int h){
        char[][][] out = new char[h][w][4];
        forEachPixel(h, w, (i, j) -> out[i][j] = toChars(arr[(i*w)+j]) );
        return out;
    }
    public static char[][] toChars(int[] inputArray){
        int size = inputArray.length;
        char[][] out = new char[size][4];
        forEachPixel(size, i -> out[i] = toChars(inputArray[i]) );
        return out;
    }
    public static char[] toChars(int inputNumber){
        char b0 = (char) (inputNumber >> 24);
        char b1 = (char) (inputNumber >> 16);
        char b2 = (char) (inputNumber >> 8);
        char b3 = (char) (inputNumber);
        return new char[] {b0, b1, b2, b3};
    }
    public static int[][] toInt2D (int[] arr, int w, int h) {
        int[][] out = new int[h][w];
        for (int i=0; i<h; i++){
            System.arraycopy(arr, i*w, out[i], 0, w);
        }
        return out;
    }
    public static int[] toInt1D (int[][] arr){
        return Arrays.stream(arr).flatMapToInt(Arrays::stream).toArray();
    }
    public static int[] toInt1D (byte[][][] arr){
        return Arrays.stream(arr).flatMap(Arrays::stream).mapToInt(ImageTools::toInt).toArray();
    }
    public static int[] toInts(byte[] byteArray){
        int intArraySize = byteArray.length/4;
        int[] outArray = new int[intArraySize];
        int[] masks = new int[] {0xFF000000, 0x000000FF, 0x0000FF00, 0x00FF0000};
        int[] offsets = new int[] {24, 0, 8, 16};

        for(int i=0; i<outArray.length; i++){
            for (int j=0; j<4; j++) {
                int bai = (i*4)+j;
                outArray[i] = (outArray[i] & ~masks[j]) | ( (byteArray[bai] << offsets[j]) & masks[j]);
            }
        }

        return outArray;
    }
    public static int toInt(byte[] fourBytes){
        int[] masks = new int[] {0xFF000000, 0x000000FF, 0x0000FF00, 0x00FF0000};
        int[] offsets = new int[] {24, 0, 8, 16};
        int out = 0;
        for (int i=0; i<4; i++){
            out = (out & ~masks[i]) | ( (fourBytes[i] << offsets[i]) & masks[i]);
        }
        return out;
    }
    public static short[][] toShort2D (short[] arr, int w, int h) {
        short[][] out = new short[h][w];
        for (int i=0; i<h; i++){
            System.arraycopy(arr, i*w, out[i], 0, w);
        }
        return out;
    }
    public static int[] toShorts(byte[] byteArray){
        int intArraySize = byteArray.length/4;
        int[] outArray = new int[intArraySize];
        int[] masks = new int[] {0xFF000000, 0x000000FF, 0x0000FF00, 0x00FF0000};
        int[] offsets = new int[] {24, 0, 8, 16};

        for(int i=0; i<outArray.length; i++){
            for (int j=0; j<4; j++) {
                int bai = (i*4)+j;
                outArray[i] = (outArray[i] & ~masks[j]) | ( (byteArray[bai] << offsets[j]) & masks[j]);
            }
        }

        return outArray;
    }
    public static int toShort(byte[] fourBytes){
        int[] masks = new int[] {0xFF000000, 0x000000FF, 0x0000FF00, 0x00FF0000};
        int[] offsets = new int[] {24, 0, 8, 16};
        int out = 0;
        for (int i=0; i<4; i++){
            out = (out & ~masks[i]) | ( (fourBytes[i] << offsets[i]) & masks[i]);
        }
        return out;
    }



    public static byte rgbToGrayscaleByte(int rgb){
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (byte) ((r+g+b)/3); // return gray
    }
    public static char rgbToGrayscaleChar(int rgb){
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (char) ((r+g+b)/3); // return gray
    }
    public static int grayscaleToRgb(byte gray){
        int[] masks = new int[] {0xFF000000, 0x000000FF, 0x0000FF00, 0x00FF0000};
        int[] offsets = new int[] {24, 0, 8, 16};
        int out = 0;
        out = (out & ~masks[0]) | ( (0xFF << offsets[0]) & masks[0]);
        for (int i=1; i<4; i++){
            out = (out & ~masks[i]) | ( (gray << offsets[i]) & masks[i]);
        }
        return out;
    }
    public static int grayscaleToRgb(char gray){
        int[] masks = new int[] {0xFF000000, 0x000000FF, 0x0000FF00, 0x00FF0000};
        int[] offsets = new int[] {24, 0, 8, 16};
        int out = 0;
        out = (out & ~masks[0]) | ( (0xFF << offsets[0]) & masks[0]);
        for (int i=1; i<4; i++){
            out = (out & ~masks[i]) | ( (gray << offsets[i]) & masks[i]);
        }
        return out;
    }
    public static float toFloat(byte in){
        return (float) ( (char) (in & 0xFF) / 255 );
    }
    public static float toFloat(char in){
        return (float) (in / 255);
    }
    public static char toChar(float in){
        return (char) (in*255);
    }
    public static char toChar(byte in){
        return (char) (in & 0xFF);
    }
    public static byte toByte(float in){
        return (byte) (in*255);
    }
    public static byte toByte(char in){
        return (byte) in;
    }


    private static byte[] compressImage(String type, int width, int height, int[] data, int targetType) throws IOException {
        MemoryImageSource mis = new MemoryImageSource(width, height, data, 0, width);
        Image im = Toolkit.getDefaultToolkit().createImage(mis);

        BufferedImage bufferedImage = new BufferedImage(width, height, targetType); // type BufferedImage.TYPE
        bufferedImage.getGraphics().drawImage(im, 0, 0, null);

        ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
        ImageIO.write(bufferedImage, type, bout);

        return bout.toByteArray();
    }
//    private static byte[] compressImage(String type, int width, int height, byte[] data) throws IOException {
//        ICC_Profile icp = new ICC_ProfileGray();
//        ICC_ColorSpace ics = new ICC_ColorSpace(icp);
//        ComponentColorModel cm = new ComponentColorModel(ics, false, false, ColorModel.BITMASK, DataBuffer.TYPE_BYTE);
//        MemoryImageSource mis = new MemoryImageSource(width, height, cm, data, 0, width);
//        Image im = Toolkit.getDefaultToolkit().createImage(mis);
//
//        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//        bufferedImage.getGraphics().drawImage(im, 0, 0, null);
//
//        ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
//        ImageIO.write(bufferedImage, type, bout);
//
//        return bout.toByteArray();
//    }

    public static byte[] getImageAsRawBytes (BufferedImage img) {
        return ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
    }
    public static int[] getImageAsRawInts (BufferedImage img) {
        return ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
    }

    public static int[] getImageAsIntRGB (BufferedImage img, int width, int height) {
        return img.getRGB(0, 0, width, height, null, 0, width);
    }




    public static void forEachPixel (int x, int y, For2DEachLambda lambda) {
        for (int i=0; i<x; i++){
            for (int j=0; j<y; j++){
                lambda.execute(i, j);
            }
        }
    }
    public static void forEachPixel (int x, For1DEachLambda lambda) {
        for (int i=0; i<x; i++){
            lambda.execute(i);
        }
    }
    public interface For1DEachLambda{
        void execute (int i);
    }
    public interface For2DEachLambda{
        void execute (int i, int j);
    }

}
