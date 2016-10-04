package game;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * The initial Android Activity, setting and initiating
 * the OpenGL ES Renderer Class @see Lesson06.java
 * 
 * @author Savas Ziplies (nea/INsanityDesign)
 */
public class Run extends Activity {

	/** The OpenGL View */
	private Surface glSurface;
	private Game game;

	/**
	 * Initiate the OpenGL View and set our own
	 * Renderer (@see Lesson06.java)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Create an Instance with this Activity
		glSurface = new Surface(this);
		//Set our own Renderer and hand the renderer this Activity Context
		game = new Game(this,glSurface);
		glSurface.setRenderer(game);
		//Set the GLSurface as View to this Activity
		setContentView(glSurface);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/**
	 * Remember to resume the glSurface
	 */
	@Override
	protected void onResume() {
		super.onResume();
		glSurface.onResume();
	}

	/**
	 * Also pause the glSurface
	 */
	@Override
	protected void onPause() {
		super.onPause();
		super.onDestroy();
		glSurface.onPause();
		game.shutDownNetworking();
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		game.shutDownNetworking();
	}
}