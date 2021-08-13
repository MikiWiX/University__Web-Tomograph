package com.example.springboot.app;

import com.example.springboot.app.tools.ImageTools;
import com.example.springboot.app.tools.TomographTools;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ImageToolsTest {

    private byte[][][] asUnsignedByte(char[][][] in){
        byte[][][] out = new byte[in.length][in[0].length][in[0][0].length];
        for (int i=0; i<in.length; i++) {
            for (int j=0; j<in[0].length; j++){
                for (int k=0; k<in[0][0].length; k++){
                    out[i][j][k] = (byte) in[i][j][k];
                }
            }
        }
        return out;
    }
    private byte[][] asUnsignedByte(char[][] in){
        byte[][] out = new byte[in.length][in[0].length];
        for (int i=0; i<in.length; i++) {
            for (int j=0; j<in[0].length; j++){
                out[i][j] = (byte) in[i][j];
            }
        }
        return out;
    }
    private byte[] asUnsignedByte(char[] in){
        byte[] out = new byte[in.length];
        for (int i=0; i<in.length; i++) {
            out[i] = (byte) in[i];
        }
        return out;
    }
    private byte asUnsignedByte(char in){
        return (byte)in;
    }

    @Test
    void byteToInt() {
        byte[] in = asUnsignedByte(new char[] {0xFA, 0x89, 0x23, 0x5E, 0x01, 0x00, 0x04, 0xFF});
        int[] out = new int[] {0xFA5E2389, 0x01FF0400};

        assertArrayEquals(out, ImageTools.toInts(in));
    }

    @Test
    void intToByte() {
        int in = 0xE4F500A1;
        byte[] out = asUnsignedByte(new char[] {0xE4, 0xF5, 0x00, 0xA1});

        assertArrayEquals(out, ImageTools.toBytes(in));
    }

    @Test
    void intToByteArray() {
        int[] in = new int[] {0xFF0900AC, 0x00AB8900, 0xFFFF7000, 0x03F5558A};
        int w=2, h=2;
        char[][][] preOut = new char[][][] { { {0xFF,0x09,0x00,0xAC}, {0x00,0xAB,0x89,0x00} },
                                            { {0xFF,0xFF,0x70,0x00}, {0x03,0xF5,0x55,0x8A} } };
        byte[][][] out = asUnsignedByte(preOut);
        byte[][][] arr = ImageTools.toByte3D(in, w, h);
        try {
            assertTrue(Arrays.deepEquals(out, arr));
        } catch (AssertionError e){
            System.out.println("Expected value:");
            System.out.println(Arrays.deepToString(out));
            System.out.println("Given value:");
            System.out.println(Arrays.deepToString(arr));
            throw e;
        }
    }

    @Test
    void conversionTest() {
        byte[] in = asUnsignedByte(new char[] {0xE4, 0xF5, 0x00, 0xA1, 0xFF, 0x00});
        char[] out0 = new char[] {0xE4, 0xF5, 0x00, 0xA1, 0xFF, 0x00};
        float[] out1 = new float[] {1, 0};
        for (int i=0; i<in.length; i++) {
            assertEquals(out0[i], ImageTools.toChar(in[i]));
            assertTrue(out1[0] > ImageTools.toFloat(in[i]) || ImageTools.toFloat(in[i]) > out1[1] );
        }
        for (int i=4; i<6; i++) {
            assertEquals(out1[i-4], ImageTools.toFloat(in[i]));
        }
    }

    @Test
    void convolutionTest() {
        char[] in = new char[] {0, 5, 10, 6, 4, 8};
        double[] kernel = new double[] {-0.5, 1.0, 0.0};

        double[] out = new double[] {0, 5, 7.5, 1, 1, 6};

        assertArrayEquals(out, TomographTools.convolveSame(in, kernel));
    }
}
