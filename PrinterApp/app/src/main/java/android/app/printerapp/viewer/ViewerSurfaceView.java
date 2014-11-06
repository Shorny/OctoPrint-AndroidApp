package android.app.printerapp.viewer;

import android.app.printerapp.viewer.Geometry.Vector;
import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

public class ViewerSurfaceView extends GLSurfaceView{
	//View Modes
	public static final int NORMAL = 0;
	public static final int XRAY = 1;
	public static final int TRANSPARENT = 2;
	public static final int LAYERS = 3;
	public static final int OVERHANG = 4;

		
	ViewerRenderer mRenderer;
	private List<DataStorage> mDataList = new ArrayList<DataStorage>();
	//Touch
	private int mMode;
	private final float TOUCH_SCALE_FACTOR_ROTATION = 90.0f / 320;  //180.0f / 320;
	private float mPreviousX;
	private float mPreviousY;
	
   // zoom rate (larger > 1.0f > smaller)
	private float pinchScale = 1.0f;

	private PointF pinchStartPoint = new PointF();
	private float pinchStartY = 0.0f;
	private float pinchStartZ = 0.0f;
	private float pinchStartDistance = 0.0f;
	private float pinchStartFactorX = 0.0f;
	private float pinchStartFactorY = 0.0f;
	private float pinchStartFactorZ = 0.0f;

	// for touch event handling
	private static final int TOUCH_NONE = 0;
	private static final int TOUCH_DRAG = 1;
	private static final int TOUCH_ZOOM = 2;
	private int touchMode = TOUCH_NONE;
		
	//Viewer modes
	public static final int ROTATION_MODE =0;
	public static final int TRANSLATION_MODE = 1;
	public static final int LIGHT_MODE = 2;
	
	private int mMovementMode;
	
	//Edition mode
	private boolean mEdition = false;
	private int mEditionMode;
	private int mRotateMode;

	
	//Edition modes
	public static final int NONE_EDITION_MODE = 0;
	public static final int MOVE_EDITION_MODE = 1;
	public static final int ROTATION_EDITION_MODE =2;
	public static final int SCALED_EDITION_MODE = 3;
	public static final int MIRROR_EDITION_MODE = 4;

	public static final int ROTATE_X = 0;
	public static final int ROTATE_Y = 1;
	public static final int ROTATE_Z = 2;

	private int mObjectPressed = -1;

    //TODO TEMP
    private float[] mCurrentAngle = {0,0,0};
	
	public ViewerSurfaceView(Context context) {
	    super(context);
	}
	public ViewerSurfaceView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	}
		
	/**
	 * 
	 * @param context Context
	 * @param data Data to render
	 * @param state Type of rendering: normal, triangle, overhang, layers
	 * @param mode Mode of rendering: do snapshot (take picture for library), dont snapshot (normal) and print_preview (gcode preview in print progress)  
	 */
	public ViewerSurfaceView(Context context, List<DataStorage> data, int state, int mode) {
		super(context);
		// Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        this.mMode = mode;
        this.mDataList = data;
		this.mRenderer = new ViewerRenderer (data, context, state, mode);
		setRenderer(mRenderer);
				
		// Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	/**
	 * Check if it is an stl model.
	 * @return
	 */
	private boolean isStl() {
		if (mDataList.size()>0)
			if (mDataList.get(0).getPathFile().endsWith(".stl") || mDataList.get(0).getPathFile().endsWith(".STL")) return true;
		
		return false;
	}
	
	/**
	 * Set the view options depending on the model
	 * @param state
	 */
	public void configViewMode (int state) {
		switch (state) {
		case (ViewerSurfaceView.NORMAL):
			setOverhang(false);
			setXray(false);
			setTransparent(false);
			break;			
		case (ViewerSurfaceView.XRAY):
			setOverhang(false);
			setXray(true);
			setTransparent(false);
			break;
		case (ViewerSurfaceView.TRANSPARENT):
			setOverhang(false);
			setXray(false);
			setTransparent(true);
			break;
		case (ViewerSurfaceView.OVERHANG):
			setOverhang(true);
			setXray(false);
			setTransparent(false);
			break;
		}
		
		requestRender();
	}
		
	/**
	 * Show/Hide back Witbox Face 
	 */
	public void showBackWitboxFace () {
		if (mRenderer.getShowBackWitboxFace()) mRenderer.showBackWitboxFace(false);
		else mRenderer.showBackWitboxFace(true);	
		requestRender();		
	}
	
	public void showRightWitboxFace () {
		if (mRenderer.getShowRightWitboxFace()) mRenderer.showRightWitboxFace(false);
		else mRenderer.showRightWitboxFace(true);
		requestRender();		
	}
	
	public void showLeftWitboxFace () {
		if (mRenderer.getShowLeftWitboxFace()) mRenderer.showLeftWitboxFace(false);
		else mRenderer.showLeftWitboxFace(true);
		requestRender();		
	}
	
	public void showDownWitboxFace () {
		if (mRenderer.getShowDownWitboxFace()) mRenderer.showDownWitboxFace(false);
		else mRenderer.showDownWitboxFace(true);
		requestRender();		
	}
	
	/**
	 * Tells the render if overhang is activated or not
	 * @param overhang 
	 */
	public void setOverhang (boolean overhang) {
		mRenderer.setOverhang(overhang);
	}
	
	/**
	 * Tell the render if transparent view is activated or not
	 * @param trans
	 */
	public void setTransparent (boolean trans) {
		mRenderer.setTransparent(trans);
	}
	
	/**
	 * Tells render if xray view (triangles view) is activated or not
	 * @param xray
	 */
	public void setXray (boolean xray) {
		mRenderer.setXray(xray);
	}
	
	/**
	 * Set edition mode
	 * @param mode MOVE_EDITION_MODE, ROTATION_EDITION_MODE, SCALED_EDITION_MODE, MIRROR_EDITION_MODE
	 */
	public void setEditionMode (int mode) {
		mEditionMode = mode;
	}
	
	/**
	 * Delete selected object
	 */
	public void deleteObject() {
		mRenderer.deleteObject(mObjectPressed);
	}
	
	/**
	 * Get the object that has been pressed
	 * @return mObjectPressed 
	 */
	public int getObjectPresed () {
		return mObjectPressed;
	}
	
	/**
	 * Set the rotation axis
	 * @param mode ROTATION_X, ROTATION_Y, ROTATION_Z
	 */
	public void setRotationVector (int mode) {
		switch (mode) {
		case ROTATE_X:
			mRotateMode = ROTATE_X;
			mRenderer.setRotationVector(new Vector (1,0,0));
			break;
		case ROTATE_Y:
			mRotateMode = ROTATE_Y;
			mRenderer.setRotationVector(new Vector (0,1,0));
			break;
		case ROTATE_Z:
			mRotateMode = ROTATE_Z;
			mRenderer.setRotationVector(new Vector (0,0,1));
			break;
		}		
	}

    /**
     * Return the current angle rotation for every axis
     * @return
     */
    public float[] getCurrentAngle(){
        return mCurrentAngle;
    }
		
	/**
	 * Rotate the object in the X axis
	 * @param angle angle to rotate
	 */
	public void rotateAngleAxisX (float angle) {
		if (mRotateMode!=ROTATE_X)	setRotationVector(ROTATE_X);

        float rotation = angle - mCurrentAngle[0];
        mCurrentAngle[0] = mCurrentAngle[0] + (angle - mCurrentAngle[0]);

		mRenderer.setRotationObject (rotation);
	    mRenderer.refreshRotatedObjectCoordinates();

	}
	
	/**
	 * Rotate the object in the Y axis
	 * @param angle angle to rotate
	 */
	public void rotateAngleAxisY (float angle) {
		if (mRotateMode!=ROTATE_Y) setRotationVector(ROTATE_Y);

        float rotation = angle - mCurrentAngle[1];
        mCurrentAngle[1] = mCurrentAngle[1] + (angle - mCurrentAngle[1]);

		mRenderer.setRotationObject (rotation);
		mRenderer.refreshRotatedObjectCoordinates();
	}
	
	/**
	 * Rotate the object in the Z axis
	 * @param angle angle to rotate
	 */
	public void rotateAngleAxisZ (float angle) {
		if (mRotateMode!=ROTATE_Z) setRotationVector(ROTATE_Z);

        float rotation = angle - mCurrentAngle[2];
        mCurrentAngle[2] = mCurrentAngle[2] + (angle - mCurrentAngle[2]);

        mRenderer.setRotationObject (rotation);
        mRenderer.refreshRotatedObjectCoordinates();
	}
	
	/**
	 * On touch events
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mMode == ViewerMainFragment.PRINT_PREVIEW) return false;

		float x = event.getX();
        float y = event.getY();
               
        float normalizedX = (event.getX() / (float) mRenderer.getWidthScreen()) * 2 - 1;
		float normalizedY = -((event.getY() / (float) mRenderer.getHeightScreen()) * 2 - 1);
								
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			// starts pinch
			case MotionEvent.ACTION_POINTER_DOWN:
				if (event.getPointerCount() >= 2) {
					pinchStartDistance = getPinchDistance(event);
					pinchStartY = mRenderer.getCameraPosY();
					pinchStartZ = mRenderer.getCameraPosZ();
					
					if (mObjectPressed!=-1) {
						pinchStartFactorX = mDataList.get(mObjectPressed).getLastScaleFactorX();
						pinchStartFactorY = mDataList.get(mObjectPressed).getLastScaleFactorY();
						pinchStartFactorZ = mDataList.get(mObjectPressed).getLastScaleFactorZ();
					}

					if (pinchStartDistance > 50f) {
						getPinchCenterPoint(event, pinchStartPoint);
						mPreviousX = pinchStartPoint.x;
						mPreviousY = pinchStartPoint.y;
						touchMode = TOUCH_ZOOM;
					}
				}
				break;				
			case MotionEvent.ACTION_DOWN:
				if (touchMode == TOUCH_NONE && event.getPointerCount() == 1) {
					int objPressed = mRenderer.objectPressed(normalizedX, normalizedY);
					if (objPressed!=-1 && isStl()) {
						mEdition = true;
						mObjectPressed=objPressed;
						ViewerMainFragment.showActionModeBar();
					} 
					touchMode = TOUCH_DRAG;
					mPreviousX = event.getX();
					mPreviousY = event.getY();
				}												
				break;			
			case MotionEvent.ACTION_MOVE:	
					float dx = x - mPreviousX;
					float dy = y - mPreviousY;
										
					if (touchMode == TOUCH_ZOOM && pinchStartDistance > 0) {
						// on pinch
						PointF pt = new PointF();
						getPinchCenterPoint(event, pt);
						
						mPreviousX = pt.x;
						mPreviousY = pt.y;
										
						pinchScale = getPinchDistance(event) / pinchStartDistance;
						
														
						if (mEdition && mEditionMode == SCALED_EDITION_MODE) {
							float fx = pinchStartFactorX*pinchScale;
							float fy = pinchStartFactorY*pinchScale;
							float fz = pinchStartFactorZ*pinchScale;

							mRenderer.scaleObject(fx,fy,fz);
						} else {
							mRenderer.setCameraPosY(pinchStartY / pinchScale);
							mRenderer.setCameraPosZ(pinchStartZ / pinchScale);						
						}

						requestRender();

						
					}else if (touchMode == TOUCH_DRAG) {
						mPreviousX = x;
					    mPreviousY = y;
					    
					    if (mEdition && mEditionMode == MOVE_EDITION_MODE) {
					    	mRenderer.dragObject(normalizedX, normalizedY);
					    } else 	dragAccordingToMode (dx,dy);
					    				    
					} 
									
					requestRender();								    
	                break;
			
			// end pinch
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:		
				if (touchMode == TOUCH_ZOOM) {
					pinchScale = 1.0f;
					pinchStartPoint.x = 0.0f;
					pinchStartPoint.y = 0.0f;
				}
								
				if(mEdition) mRenderer.changeTouchedState();

				touchMode = TOUCH_NONE;
				requestRender();			
				break;				
		}
		return true;
	}
	
	/**
	 * Exit edition mode.
	 * Set object pressed to -1 (no object pressed)
	 * Set mEditionMode to NONE_EDITION_MODE
	 * Change state of the object (which means the colour of the models in the plate will probably change)
	 */
	public void exitEditionMode () {
		mEdition = false;
		mEditionMode = NONE_EDITION_MODE;
		//We can exit edition mode at clicking in the menu or at deleting a model. If the model has been deleted, it is possible that
		//mRenderer.exitEditionModel fails because of the size of the arrays.
		mObjectPressed = -1;
		mRenderer.setObjectPressed(mObjectPressed);
		mRenderer.changeTouchedState();
    	
    	requestRender();
	}
		
	/**
	 * It rotates the plate (ROTATION or TRANSLATION) 
	 * @param dx movement on x axis
	 * @param dy movement on y axis
	 */
	private void dragAccordingToMode (float dx, float dy) {
		switch (mMovementMode) {
		case ROTATION_MODE:
			doRotation (dx,dy);
			break;
		case TRANSLATION_MODE:
			doTranslation (dx,dy);
			break;
		}
	}
	
	/**
	 * Mirror option
	 */
	public void doMirror () {
		float fx = mDataList.get(mObjectPressed).getLastScaleFactorX();
		float fy = mDataList.get(mObjectPressed).getLastScaleFactorY();
		float fz = mDataList.get(mObjectPressed).getLastScaleFactorZ();
		
		mRenderer.scaleObject(-1*fx, fy, fz);
		requestRender();
	}
	
	/**
	 * Do rotation (plate rotation, not model rotation)
	 * @param dx movement on x axis
	 * @param dy movement on y axis
	 */
	private void doRotation (float dx, float dy) {              
        mRenderer.setSceneAngleX(dx*TOUCH_SCALE_FACTOR_ROTATION);
        mRenderer.setSceneAngleY(dy*TOUCH_SCALE_FACTOR_ROTATION);		
	} 
	
	/**
	 * Do rotation (plate rotation, not model rotation)
	 * @param dx movement on x axis
	 * @param dy movement on y axis
	 */
	private void doTranslation(float dx, float dy) {
		mRenderer.setCenterX(-dx);
		mRenderer.setCenterZ(dy); //
	}
	
	/**
	 * Movement mode (plate)
	 * @param mode
	 */
	public void setMovementMode (int mode) {
		mMovementMode = mode;
	}
	
	/**
	 * Get distanced pinched
	 * @param event
	 * @return
	 */
	private float getPinchDistance(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	
	/**
	 * Get center point
	 * @param event
	 * @param pt pinched point
	 */
	private void getPinchCenterPoint(MotionEvent event, PointF pt) {
		pt.x = (event.getX(0) + event.getX(1)) * 0.5f;
		pt.y = (event.getY(0) + event.getY(1)) * 0.5f;
	}
}

