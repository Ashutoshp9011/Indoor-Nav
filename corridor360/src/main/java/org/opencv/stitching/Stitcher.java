//
// This file is auto-generated. Please don't modify it!
//
package org.opencv.stitching;

import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.utils.Converters;

// C++: class Stitcher
/**
 * High level image stitcher.
 */
public class Stitcher {

    protected final long nativeObj;
    protected Stitcher(long addr) { nativeObj = addr; }

    public long getNativeObjAddr() { return nativeObj; }

    // internal usage only
    public static Stitcher __fromPtr__(long addr) { return new Stitcher(addr); }

    // C++: enum Mode
    public static final int
            PANORAMA = 0,
            SCANS = 1;


    // C++: enum Status
    public static final int
            OK = 0,
            ERR_NEED_MORE_IMGS = 1,
            ERR_HOMOGRAPHY_EST_FAIL = 2,
            ERR_CAMERA_PARAMS_ADJUST_FAIL = 3;


    //
    // C++: static Ptr_Stitcher cv::Stitcher::create(Stitcher_Mode mode = Stitcher::PANORAMA)
    //

    public static Stitcher create(int mode) {
        return Stitcher.__fromPtr__(create_0(mode));
    }

    public static Stitcher create() {
        return Stitcher.__fromPtr__(create_1());
    }


    //
    // C++: Stitcher_Status cv::Stitcher::stitch(vector_Mat images, Mat& pano)
    //

    public int stitch(List<Mat> images, Mat pano) {
        Mat images_mat = Converters.vector_Mat_to_Mat(images);
        return stitch_0(nativeObj, images_mat.nativeObj, pano.nativeObj);
    }


    @Override
    protected void finalize() throws Throwable {
        delete(nativeObj);
    }



    // C++: static Ptr_Stitcher cv::Stitcher::create(Stitcher_Mode mode = Stitcher::PANORAMA)
    private static native long create_0(int mode);
    private static native long create_1();

    // C++: Stitcher_Status cv::Stitcher::stitch(vector_Mat images, Mat& pano)
    private static native int stitch_0(long nativeObj, long images_mat_nativeObj, long pano_nativeObj);

    // native support for java finalize()
    private static native void delete(long nativeObj);

}
