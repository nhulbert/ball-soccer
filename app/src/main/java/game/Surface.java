package game;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

// The surfaceview class, only used to handle raw input

public class Surface extends GLSurfaceView {	
	public ArrayList<Boolean> processDown = new ArrayList<>(Arrays.asList(false, false));
	public ArrayList<Boolean> processUp = new ArrayList<>(Arrays.asList(false, false));
	
	public ArrayList<Point2D> downpos = new ArrayList<>(Arrays.asList(new Point2D(), new Point2D()));
	public ArrayList<Point2D> uppos = new ArrayList<>(Arrays.asList(new Point2D(), new Point2D()));
	
	public ArrayList<Point2D> curpos = new ArrayList<>(Arrays.asList(new Point2D(), new Point2D()));
	
	public ArrayList<Integer> pointerIDs = new ArrayList<>(Arrays.asList(-1,-1));
	
	public Surface(Context context) {
		super(context);
		setEGLContextClientVersion(2);
	}

	public Surface(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public boolean onTouchEvent(MotionEvent e){
		int ind = e.getActionIndex();
		
		
		if (ind < 2){
			for (int i = 0; i < e.getPointerCount() && i < 2; i++){
				int id = e.getPointerId(i);
				if (id < 2) {
					curpos.get(id).x = e.getX(i);
					curpos.get(id).y = e.getY(i);
				}
			}
			updateIDs(e);
			
			int pointerInd = pointerIDs.get(ind);
			
			/*if (e.getAction() != MotionEvent.ACTION_MOVE){
				System.out.print("Garbage");
			}*/
			
			
			
			switch(e.getActionMasked()){
				case MotionEvent.ACTION_UP:
					if (pointerInd < 2) {
						uppos.get(pointerInd).x = e.getX(ind);
						uppos.get(pointerInd).y = e.getY(ind);
						processUp.set(pointerInd, true);
					}
					break;
					
				case MotionEvent.ACTION_DOWN:
				{
					if (pointerInd < 2) {
						downpos.get(pointerInd).x = e.getX(ind);
						downpos.get(pointerInd).y = e.getY(ind);
						processDown.set(pointerInd, true);
					}
					break;
				}
				case MotionEvent.ACTION_POINTER_UP:
				{
					if (pointerInd < 2) {
						uppos.get(pointerInd).x = e.getX(ind);
						uppos.get(pointerInd).y = e.getY(ind);
						processUp.set(pointerInd, true);
					}
					break;
				}
				case MotionEvent.ACTION_POINTER_DOWN:
				{
					if (pointerInd < 2) {
						downpos.get(pointerInd).x = e.getX(ind);
						downpos.get(pointerInd).y = e.getY(ind);
						processDown.set(pointerInd, true);
					}
				}
			}
		}
		
		return true;
	}
	
	public void updateIDs(MotionEvent e){
		for (int i = 0; i < pointerIDs.size(); i++){
			if (i < e.getPointerCount()){
				int id = e.getPointerId(i);
				pointerIDs.set(i, id);
			}
			else{
				pointerIDs.set(i, -1);
			}
		}
	}
}
