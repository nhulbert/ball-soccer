package game;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

// The main OpenGL activity

public class Run extends Activity {
	private Surface glSurface;
	private Game game;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		glSurface = new Surface(this);
		game = new Game(this,glSurface);
		glSurface.setRenderer(game);
		setContentView(glSurface);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onResume() {
		super.onResume();
		glSurface.onResume();
	}

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