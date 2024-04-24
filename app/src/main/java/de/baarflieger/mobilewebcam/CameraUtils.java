import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;


public class CameraUtils {

    public static int getNumberOfCameras(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            return cameraManager.getCameraIdList().length;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return 0; // Return 0 if camera access is not available.
        }
    }

    public static boolean isFrontCamera(Context context, int camIdx) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            if (camIdx >= 0 && camIdx < cameraIdList.length) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIdList[camIdx]);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                return lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getFrontFacingCameraIndex(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (int i = 0; i < cameraIdList.length; i++) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraIdList[i]);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return i; // Return the index of the front-facing camera
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return -1; // Return -1 if no front-facing camera is found
    }

}