package game;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.mockitleaguemassive.R;

public class Draw {
	
	
	public static int[] textures = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	
	public static ArrayList<Integer> texWidths;
	public static ArrayList<Integer> texHeights;
	
	private static final String cubeMapVertexShaderCode = "" +
			"uniform mat4 uMVPMatrix; \n" +
			"attribute vec3 a_Position; \n" +
			"varying vec3 v_TexCoordinate; \n" +
			
			"void main() \n" +
			"{\n" +
				"gl_Position = uMVPMatrix * vec4(a_Position,1.0); \n" +
				"gl_Position = gl_Position.xyww; \n" +
				"v_TexCoordinate = a_Position; \n" +
			"}";
	
	private static final String cubeMapFragmentShaderCode = "" +
			"uniform samplerCube texture; \n" +
			"varying vec3 v_TexCoordinate; \n" +
			
			"void main() \n" +
			"{\n" +
				"gl_FragColor = textureCube(texture, v_TexCoordinate);\n"+
			"}";
	
	private static final String vertexShaderCode =
			    "uniform mat4 uMVPMatrix; \n"+       // combined model/view/projection matrix.
	    		"uniform mat4 uMVMatrix; \n"+       // combined model/view matrix.
 
				"attribute vec4 a_Position \n;"+      // Per-vertex position
				"attribute vec3 a_Normal \n;"+      // Per-vertex normal
				"attribute vec2 a_TexCoordinate; \n"+ // Per-vertex texture coordinate
 
				"varying vec3 v_Position; \n"+
				"varying vec3 v_Normal; \n"+
				"varying vec2 v_TexCoordinate; \n"+

				"void main() \n"+
				"{ \n"+
    				"v_Position = vec3(uMVMatrix * a_Position); \n"+

    				"v_TexCoordinate = a_TexCoordinate; \n"+

    				"v_Normal = vec3(uMVMatrix * vec4(a_Normal, 0.0)); \n"+

    				"gl_Position = uMVPMatrix * a_Position; \n"+
				"}";

	private static final String vertexShaderCodeFullbright =
			"uniform mat4 uMVPMatrix; \n"+
					"uniform mat4 uMVMatrix; \n"+

					"attribute vec4 a_Position \n;"+
					"attribute vec3 a_Normal \n;"+
					"attribute vec2 a_TexCoordinate; \n"+

					"varying vec3 v_Position; \n"+
					"varying vec3 v_Normal; \n"+
					"varying vec2 v_TexCoordinate; \n"+


					"void main() \n"+
					"{ \n"+
					"v_Position = vec3(uMVMatrix * a_Position); \n"+

					"v_TexCoordinate = a_TexCoordinate; \n"+

					"v_Normal = vec3(uMVMatrix * vec4(a_Normal, 0.0)); \n"+

					"gl_Position = uMVPMatrix * a_Position; \n"+

					"gl_Position.z = -1.0; \n"+

					"}";

    private static final String fragmentShaderCode =
    		"precision mediump float; \n"+
			"uniform vec3 u_LightPos; \n"+
			"uniform sampler2D u_Texture; \n"+

			"varying vec3 v_Position; \n"+
    		"varying vec3 v_Normal; \n"+
    		"varying vec2 v_TexCoordinate; \n"+

			"void main() \n"+
				"{ \n"+

				"float distance = length(u_LightPos - v_Position); \n"+

				"vec3 lightVector = normalize(u_LightPos - v_Position); \n"+

				"vec3 v_Normal_Flipped; \n"+
				
				"if (!gl_FrontFacing){ \n"+
					"v_Normal_Flipped = vec3(v_Normal[0], v_Normal[1], v_Normal[2]); \n"+
				"} \n"+
				"else{ \n"+
					"v_Normal_Flipped = vec3(-v_Normal[0], -v_Normal[1], -v_Normal[2]); \n"+
				"} \n"+

				"float diffuse = max(dot(v_Normal_Flipped, lightVector), 0.0); \n"+

				"diffuse = diffuse * (1.0 / (1.0 + (0.03 * distance))); \n"+

				"diffuse = diffuse + 0.3; \n"+

				"vec4 res = texture2D(u_Texture, v_TexCoordinate);"+

				"res.xyz *= diffuse;"+

				"gl_FragColor = res; \n"+
			"}";

	private static final String fragmentShaderCodeFullbright =
			"precision mediump float; \n"+

					"uniform vec3 u_LightPos; \n"+

					"uniform sampler2D u_Texture; \n"+

					"varying vec3 v_Position; \n"+

					"varying vec3 v_Normal; \n"+

					"varying vec2 v_TexCoordinate; \n"+

					"void main() \n"+

					"{ \n"+

					"float distance = length(u_LightPos - v_Position); \n"+

					"vec3 lightVector = normalize(u_LightPos - v_Position); \n"+

					"vec3 v_Normal_Flipped; \n"+

					"if (!gl_FrontFacing){ \n"+

					"v_Normal_Flipped = vec3(v_Normal[0], v_Normal[1], v_Normal[2]); \n"+

					"} \n"+

					"else{ \n"+

					"v_Normal_Flipped = vec3(-v_Normal[0], -v_Normal[1], -v_Normal[2]); \n"+

					"} \n"+

					"float diffuse = max(dot(v_Normal_Flipped, lightVector), 0.0); \n"+

					"diffuse = diffuse * (1.0 / (1.0 + (0.10 * distance))); \n"+

					"diffuse = diffuse + 0.3; \n"+

					"gl_FragColor = texture2D(u_Texture, v_TexCoordinate); \n"+
					"}";
    
	private static final String reflectVertexShaderCode =
		    "uniform mat4 uMVPMatrix; \n"+
    		"uniform mat4 uMVMatrix; \n"+
    		"uniform mat4 uMMatrix; \n"+
    		
			"attribute vec4 a_Position \n;"+
			"attribute vec3 a_Normal \n;"+
			"attribute vec2 a_TexCoordinate; \n"+

			"varying vec3 v_aPosition; \n"+
			"varying vec3 v_Position; \n"+
			"varying vec3 v_Normal; \n"+
			"varying vec2 v_TexCoordinate; \n"+
			"varying vec3 v_wPosition; \n"+
			"varying vec3 v_wNormal; \n"+

			"void main() \n"+
			"{ \n"+

				"v_aPosition = vec3(a_Position); \n"+

				"v_Position = vec3(uMVMatrix * a_Position); \n"+

				"v_wPosition = vec3(uMMatrix * a_Position); \n"+

				"v_TexCoordinate = a_TexCoordinate; \n"+

				"v_Normal = vec3(uMVMatrix * vec4(a_Normal, 0.0)); \n"+

				"v_wNormal = vec3(mat3(uMMatrix) * a_Normal); \n"+

				"gl_Position = uMVPMatrix * a_Position; \n"+
			"}";
    
	
    private static final String reflectFragmentShaderCode =
    		"precision mediump float; \n"+
			"uniform vec3 u_LightPos; \n"+
			"uniform vec3 u_camPos; \n"+
			"uniform sampler2D u_Texture; \n"+
			"uniform samplerCube u_Cube; \n"+
			
			"varying vec3 v_aPosition; \n"+
			"varying vec3 v_Position; \n"+
    		"varying vec3 v_wPosition; \n"+
			"varying vec3 v_Normal; \n"+
    		"varying vec2 v_TexCoordinate; \n"+
    		"varying vec3 v_wNormal; \n"+

			"void main() \n"+
				"{ \n"+

				//"float d = length(v_aPosition.xz); \n"+
				//"vec3 temp; \n" +
				//"temp = vec3(0,1,0); \n" +
				//"temp = normalize(vec3((25.0 * d - 20.0) * normalize(v_aPosition.xz), 2.0)); \n" +
				//"v_Normal = temp.xzy; \n"+

				"float distance = length(u_LightPos - v_Position); \n"+
				
				"vec3 v_Normal_Flipped; \n" +
				"vec3 v_wNormal_Flipped; \n"+
				
				"if (!gl_FrontFacing){ \n"+
					"v_Normal_Flipped = v_Normal; \n"+
					"v_wNormal_Flipped = v_wNormal; \n"+
				"} \n"+
				"else{ \n"+
					"v_Normal_Flipped = -v_Normal; \n"+
					"v_wNormal_Flipped = -v_wNormal; \n"+
				"} \n"+
					
				"vec3 lightVector = normalize(u_LightPos - v_Position); \n"+

				"vec3 incident = normalize(v_wPosition - u_camPos); \n"+

				"vec3 reflected = reflect(incident, normalize(v_wNormal_Flipped)); \n"+

				"vec4 color = textureCube(u_Cube, reflected); \n"+

				"float diffuse = max(dot(v_Normal_Flipped, lightVector), 0.0); \n"+

				"diffuse = diffuse * (1.0 / (1.0 + (0.03 * distance))); \n"+

				"diffuse = diffuse + 0.3; \n"+

				"vec4 spec = texture2D(u_Texture, v_TexCoordinate);"+

				"spec.xyz *= diffuse;"+

				"gl_FragColor = 0.1*color + spec; \n"+
				"}";
	
    

    private static int mProgram;

	private static int mFullbrightProgram;

	private static int mCubeMapProgram;
    
	private static int mRProgram;	

	static final int POSITION_DATA_SIZE = 3;	

	static final int NORMAL_DATA_SIZE = 3;

	static final int TEXTURE_COORDINATE_DATA_SIZE = 2;

	static final int BYTES_PER_FLOAT = 4;
	
	static float[] mLightPosInWorldSpace;

	static float camposx;
	static float camposy;
	static float camposz;

	public static void initialize(Context context, float[] mLightPosInWorldSpace){
        loadGLTextures(context);
        Draw.mLightPosInWorldSpace = mLightPosInWorldSpace;
        int vertexShader = Draw.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
		int vertexShaderFullbright = Draw.loadShader(
				GLES20.GL_VERTEX_SHADER,
				vertexShaderCodeFullbright);
        int fragmentShader = Draw.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);
        int fragmentShaderFullbright = Draw.loadShader(
				GLES20.GL_FRAGMENT_SHADER,
				fragmentShaderCodeFullbright);
        int cubeMapVertexShader = Draw.loadShader(
                GLES20.GL_VERTEX_SHADER,
                cubeMapVertexShaderCode);
        int cubeMapFragmentShader = Draw.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                cubeMapFragmentShaderCode);

        int rvertexShader = Draw.loadShader(
                GLES20.GL_VERTEX_SHADER,
                reflectVertexShaderCode);
        int rfragmentShader = Draw.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                reflectFragmentShaderCode);
        
        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

		mFullbrightProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mFullbrightProgram, vertexShaderFullbright);
		GLES20.glAttachShader(mFullbrightProgram, fragmentShaderFullbright);
		GLES20.glLinkProgram(mFullbrightProgram);

		mCubeMapProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mCubeMapProgram, cubeMapVertexShader);
        GLES20.glAttachShader(mCubeMapProgram, cubeMapFragmentShader);
        GLES20.glLinkProgram(mCubeMapProgram);
        
        mRProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mRProgram, rvertexShader);
        GLES20.glAttachShader(mRProgram, rfragmentShader);
        GLES20.glLinkProgram(mRProgram);
	}
	
	public static void setLightPos(float[] pos){
		Draw.mLightPosInWorldSpace = pos;
	}
	
   	private static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        
        checkGlError("Compiling Shader");
        return shader;
    }
	
    static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("GL Renderer", glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
   	
	public static void draw(int vertBufInd, int faceBufInd, int indexCount, float[] mViewMatrix, float[] mProjectionMatrix, float[] mModelMatrix, int texnum){
	    int mPositionHandle;
	    int mNormalHandle;
	    int mTexCoorHandle;
	    int mLightPosHandle;
	    int mTexHandle;
	    int mMVPMatrixHandle;
	    int mMVMatrixHandle;
		
		final int stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE + TEXTURE_COORDINATE_DATA_SIZE) * BYTES_PER_FLOAT;
		
		float[] mMVPMatrix = new float[16];
		float[] mMVMatrix = new float[16];
		
		float[] mLightPosInViewSpace = new float[4];
		
		Matrix.multiplyMV(mLightPosInViewSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);
		
		Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
 		// This multiplies the modelview matrix by the projection matrix,
 		// and stores the result in the MVP matrix
 		// (which now contains model * view * projection).
 		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);
		
		 // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        
        checkGlError("Using Program");
        
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgram, "a_Normal");
        mTexCoorHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
        
        mTexHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgram, "u_LightPos");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVMatrix");
        
        checkGlError("Getting Handles");
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		// Bind the texture to this unit.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[texnum]);
		GLES20.glUniform1i(mTexHandle, 0);
		
		// Tell the texture uniform sampler to use this texture in the
		// shader by binding to texture unit 0.
		//GLES20.glUniform1i(, 0);
        
		GLES20.glUniform3f(mLightPosHandle, mLightPosInViewSpace[0], mLightPosInViewSpace[1], mLightPosInViewSpace[2]);
		
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBufInd);
        
     // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        
     // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                stride, 0);
        
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        
        GLES20.glVertexAttribPointer(
        		mNormalHandle, 3,
        		GLES20.GL_FLOAT, false,
        		stride, 3*4);
        
        GLES20.glEnableVertexAttribArray(mTexCoorHandle);
        
        GLES20.glVertexAttribPointer(
        		mTexCoorHandle, 2,
        		GLES20.GL_FLOAT, false,
        		stride, (3+3)*4);
        
     // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
        checkGlError("Passing the Matrices in");
        
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, faceBufInd);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
        
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mNormalHandle);
        GLES20.glDisableVertexAttribArray(mTexCoorHandle);
        
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	public static void drawFullbright(int vertBufInd, int faceBufInd, int indexCount, float[] mViewMatrix, float[] mProjectionMatrix, float[] mModelMatrix, int texnum){
		int mPositionHandle;
		int mNormalHandle;
		int mTexCoorHandle;
		int mLightPosHandle;
		int mTexHandle;
		int mMVPMatrixHandle;
		int mMVMatrixHandle;

		final int stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE + TEXTURE_COORDINATE_DATA_SIZE) * BYTES_PER_FLOAT;

		float[] mMVPMatrix = new float[16];
		float[] mMVMatrix = new float[16];

		float[] mLightPosInViewSpace = new float[4];

		Matrix.multiplyMV(mLightPosInViewSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

		Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
		// This multiplies the modelview matrix by the projection matrix,
		// and stores the result in the MVP matrix
		// (which now contains model * view * projection).
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);

		GLES20.glDepthFunc(GLES20.GL_ALWAYS);

		// Add program to OpenGL environment
		GLES20.glUseProgram(mFullbrightProgram);

		checkGlError("Using Program");

		// get handle to vertex shader's vPosition member
		mPositionHandle = GLES20.glGetAttribLocation(mFullbrightProgram, "a_Position");
		mNormalHandle = GLES20.glGetAttribLocation(mFullbrightProgram, "a_Normal");
		mTexCoorHandle = GLES20.glGetAttribLocation(mFullbrightProgram, "a_TexCoordinate");

		mTexHandle = GLES20.glGetUniformLocation(mFullbrightProgram, "u_Texture");
		mLightPosHandle = GLES20.glGetUniformLocation(mFullbrightProgram, "u_LightPos");
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mFullbrightProgram, "uMVPMatrix");
		mMVMatrixHandle = GLES20.glGetUniformLocation(mFullbrightProgram, "uMVMatrix");

		checkGlError("Getting Handles");

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		// Bind the texture to this unit.
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[texnum]);
		GLES20.glUniform1i(mTexHandle, 0);

		// Tell the texture uniform sampler to use this texture in the
		// shader by binding to texture unit 0.
		//GLES20.glUniform1i(, 0);

		GLES20.glUniform3f(mLightPosHandle, mLightPosInViewSpace[0], mLightPosInViewSpace[1], mLightPosInViewSpace[2]);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBufInd);

		// Enable a handle to the triangle vertices
		GLES20.glEnableVertexAttribArray(mPositionHandle);

		// Prepare the triangle coordinate data
		GLES20.glVertexAttribPointer(
				mPositionHandle, 3,
				GLES20.GL_FLOAT, false,
				stride, 0);

		GLES20.glEnableVertexAttribArray(mNormalHandle);

		GLES20.glVertexAttribPointer(
				mNormalHandle, 3,
				GLES20.GL_FLOAT, false,
				stride, 3*4);

		GLES20.glEnableVertexAttribArray(mTexCoorHandle);

		GLES20.glVertexAttribPointer(
				mTexCoorHandle, 2,
				GLES20.GL_FLOAT, false,
				stride, (3+3)*4);

		// Apply the projection and view transformation
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
		checkGlError("Passing the Matrices in");

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, faceBufInd);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);

		GLES20.glDisableVertexAttribArray(mPositionHandle);
		GLES20.glDisableVertexAttribArray(mNormalHandle);
		GLES20.glDisableVertexAttribArray(mTexCoorHandle);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
	}

	private static void loadGLTextures(Context context){
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glEnable(GLES20.GL_BLEND);

		ArrayList<InputStream> in = new ArrayList<>();

		ArrayList<Bitmap> bitmaps = new ArrayList<>();

		ArrayList<Integer> types = new ArrayList<>();
		
		types.add(GL10.GL_TEXTURE_2D);//0
		types.add(GL10.GL_TEXTURE_2D);//1
		types.add(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X);//2
		types.add(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X);
		types.add(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y);
		types.add(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y);
		types.add(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z);
		types.add(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z);
		types.add(GL10.GL_TEXTURE_2D);//3  decimal 0
		types.add(GL10.GL_TEXTURE_2D);
		types.add(GL10.GL_TEXTURE_2D);
		types.add(GL10.GL_TEXTURE_2D);
		types.add(GL10.GL_TEXTURE_2D);
		types.add(GL10.GL_TEXTURE_2D);
		types.add(GL10.GL_TEXTURE_2D);
		types.add(GL10.GL_TEXTURE_2D);
		types.add(GL10.GL_TEXTURE_2D);
		types.add(GL10.GL_TEXTURE_2D);//12 decimal 9
		types.add(GL10.GL_TEXTURE_2D);//13 green
		types.add(GL10.GL_TEXTURE_2D);//14 blue
		types.add(GL10.GL_TEXTURE_2D);//15
		types.add(GL10.GL_TEXTURE_2D);//16
		types.add(GL10.GL_TEXTURE_2D);//17
		types.add(GL10.GL_TEXTURE_2D);//18


		in.add(context.getResources().openRawResource(R.drawable.red));
		in.add(context.getResources().openRawResource(R.drawable.grass));
		in.add(context.getResources().openRawResource(R.drawable.skyzn));
		in.add(context.getResources().openRawResource(R.drawable.skyzp));
		in.add(context.getResources().openRawResource(R.drawable.skyyn));
		in.add(context.getResources().openRawResource(R.drawable.skyyp));
		in.add(context.getResources().openRawResource(R.drawable.skyxp));
		in.add(context.getResources().openRawResource(R.drawable.skyxn));
		in.add(context.getResources().openRawResource(R.drawable.d0));
		in.add(context.getResources().openRawResource(R.drawable.d1));
		in.add(context.getResources().openRawResource(R.drawable.d2));
		in.add(context.getResources().openRawResource(R.drawable.d3));
		in.add(context.getResources().openRawResource(R.drawable.d4));
		in.add(context.getResources().openRawResource(R.drawable.d5));
		in.add(context.getResources().openRawResource(R.drawable.d6));
		in.add(context.getResources().openRawResource(R.drawable.d7));
		in.add(context.getResources().openRawResource(R.drawable.d8));
		in.add(context.getResources().openRawResource(R.drawable.d9));
		in.add(context.getResources().openRawResource(R.drawable.green));
		in.add(context.getResources().openRawResource(R.drawable.blue));

		{
			Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			bitmap.eraseColor(Color.WHITE);

			Paint textPaint = new Paint();
			textPaint.setTextSize(64);
			textPaint.setAntiAlias(true);
			textPaint.setARGB(0xff, 0xff, 0x00, 0x00);
			canvas.drawText("Client", 16,112, textPaint);

			bitmaps.add(bitmap);
		}

		{
			Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			bitmap.eraseColor(Color.RED);

			Paint textPaint = new Paint();
			textPaint.setTextSize(64);
			textPaint.setAntiAlias(true);
			textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
			canvas.drawText("Client", 16,112, textPaint);

			bitmaps.add(bitmap);
		}

		{
			Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			bitmap.eraseColor(Color.WHITE);

			Paint textPaint = new Paint();
			textPaint.setTextSize(64);
			textPaint.setAntiAlias(true);
			textPaint.setARGB(0xff, 0xff, 0x00, 0x00);
			canvas.drawText("Server", 16,112, textPaint);

			bitmaps.add(bitmap);
		}

		{
			Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			bitmap.eraseColor(Color.RED);

			Paint textPaint = new Paint();
			textPaint.setTextSize(64);
			textPaint.setAntiAlias(true);
			textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
			canvas.drawText("Server", 16,112, textPaint);

			bitmaps.add(bitmap);
		}

		int bindCount = 0;
		int i;
		for (i = 0; i < in.size(); i++){
	        texWidths = new ArrayList<>();
	        texHeights = new ArrayList<>();
			
			InputStream is = in.get(i);
			
			Bitmap bitmap = null;
			try {
				bitmap = BitmapFactory.decodeStream(is);
	
			} finally {
				try {
					is.close();
				} catch (IOException ignored) {
				}
			}
			
			int type = types.get(i);
			if (type != GL10.GL_TEXTURE_2D){
				type = GLES20.GL_TEXTURE_CUBE_MAP;
			}
			
			if (types.get(i) == GL10.GL_TEXTURE_2D || types.get(i) == GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X){
				GLES20.glGenTextures(1, textures, bindCount);
				GLES20.glBindTexture(type, textures[bindCount++]);
			}



			//Create Bilinear Filtered Texture
			if (types.get(i) == GL10.GL_TEXTURE_2D) GLES20.glTexParameterf(type, GL10.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST);
			else GLES20.glTexParameterf(type, GL10.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(type, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

			GLES20.glTexParameterf(type, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
			GLES20.glTexParameterf(type, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

			GLUtils.texImage2D(types.get(i), 0, bitmap, 0);

			if (types.get(i) == GL10.GL_TEXTURE_2D){
				GLES20.glGenerateMipmap(GL10.GL_TEXTURE_2D);
			}

			texWidths.add(bitmap.getWidth());
			texHeights.add(bitmap.getHeight());

			bitmap.recycle();
		}
		for (int h=0; h<bitmaps.size(); h++){
			Bitmap bitmap = bitmaps.get(h);

			int type = types.get(i);
			if (type != GL10.GL_TEXTURE_2D){
				type = GLES20.GL_TEXTURE_CUBE_MAP;
			}

			if (type == GL10.GL_TEXTURE_2D || type == GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X){
				GLES20.glGenTextures(1, textures, bindCount);
				GLES20.glBindTexture(type, textures[bindCount++]);
			}

			GLES20.glTexParameterf(type, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
			GLES20.glTexParameterf(type, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

			GLES20.glTexParameterf(type, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
			GLES20.glTexParameterf(type, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

			GLUtils.texImage2D(types.get(i), 0, bitmap, 0);

			texWidths.add(bitmap.getWidth());
			texHeights.add(bitmap.getHeight());

			bitmap.recycle();

			i++;
		}
    }
	
	public static void drawSkybox(int vertBufInd, int faceBufInd, int indexCount, float[] mViewMatrix, float[] mProjectionMatrix, int texnum){
	    int mCMPositionHandle;
		int mCMMVPMatrixHandle;
		
		final int stride = (POSITION_DATA_SIZE) * BYTES_PER_FLOAT;
		
		float[] mViewDirMat = Arrays.copyOf(mViewMatrix,16);
		mViewDirMat[3] = 0;
		mViewDirMat[7] = 0;
		mViewDirMat[11] = 0;
		mViewDirMat[12] = 0;
		mViewDirMat[13] = 0;
		mViewDirMat[14] = 0;
 		
		float[] mMVPMatrix = new float[16];
		
 		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewDirMat, 0);
		
 		GLES20.glValidateProgram(mCubeMapProgram);
 		
 		checkGlError("Using Program");
        GLES20.glUseProgram(mCubeMapProgram);
        
        checkGlError("Using Program");

        mCMPositionHandle = GLES20.glGetAttribLocation(mCubeMapProgram, "a_Position");
        mCMMVPMatrixHandle = GLES20.glGetUniformLocation(mCubeMapProgram, "uMVPMatrix");
		int mCMTextureHandle = GLES20.glGetUniformLocation(mCubeMapProgram, "texture");
        checkGlError("Getting Handles");
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textures[texnum]);
		GLES20.glUniform1i(mCMTextureHandle, 0);
		
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBufInd);

        GLES20.glEnableVertexAttribArray(mCMPositionHandle);

        GLES20.glVertexAttribPointer(
                mCMPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                stride, 0);

        GLES20.glUniformMatrix4fv(mCMMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        checkGlError("Passing the Matrices in");
        
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, faceBufInd);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
        
        GLES20.glDisableVertexAttribArray(mCMPositionHandle);
        
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	public static void drawReflection(int vertBufInd, int faceBufInd, int indexCount, float[] mViewMatrix, float[] mProjectionMatrix, float[] mModelMatrix, int texnum, int refnum){
	    int mPositionHandle;
	    int mNormalHandle;
	    int mTexCoorHandle;
	    int mLightPosHandle;
	    int mTexHandle;
	    int mCubeTexHandle;
	    int mMVPMatrixHandle;
	    int mMVMatrixHandle;
	    int mMMatrixHandle;
	    int mCamPosHandle;
		
		final int stride = (POSITION_DATA_SIZE + NORMAL_DATA_SIZE + TEXTURE_COORDINATE_DATA_SIZE) * BYTES_PER_FLOAT;
		
		float[] mMVPMatrix = new float[16];
		float[] mMVMatrix = new float[16];
		
		float[] mLightPosInViewSpace = new float[4];
		
		Matrix.multiplyMV(mLightPosInViewSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);
		
		Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

 		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);

        GLES20.glUseProgram(mRProgram);
        
        checkGlError("Using Program");

        mPositionHandle = GLES20.glGetAttribLocation(mRProgram, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mRProgram, "a_Normal");
        mTexCoorHandle = GLES20.glGetAttribLocation(mRProgram, "a_TexCoordinate");
        
        mTexHandle = GLES20.glGetUniformLocation(mRProgram, "u_Texture");
        mCubeTexHandle = GLES20.glGetUniformLocation(mRProgram, "u_Cube");
        mLightPosHandle = GLES20.glGetUniformLocation(mRProgram, "u_LightPos");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mRProgram, "uMVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mRProgram, "uMVMatrix");
        mMMatrixHandle = GLES20.glGetUniformLocation(mRProgram, "uMMatrix");
        mCamPosHandle = GLES20.glGetUniformLocation(mRProgram, "u_camPos");
        
        
        
        checkGlError("Getting Handles");
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[texnum]);
		GLES20.glUniform1i(mTexHandle, 0);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		
		GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textures[refnum]);
		GLES20.glUniform1i(mCubeTexHandle, 1);
        
		GLES20.glUniform3f(mLightPosHandle, mLightPosInViewSpace[0], mLightPosInViewSpace[1], mLightPosInViewSpace[2]);
		GLES20.glUniform3f(mCamPosHandle, camposx, camposy, camposz);
		
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertBufInd);

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(
                mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                stride, 0);
        
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        
        GLES20.glVertexAttribPointer(
        		mNormalHandle, 3,
        		GLES20.GL_FLOAT, false,
        		stride, 3*4);
        
        GLES20.glEnableVertexAttribArray(mTexCoorHandle);
        
        GLES20.glVertexAttribPointer(
        		mTexCoorHandle, 2,
        		GLES20.GL_FLOAT, false,
        		stride, (3+3)*4);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(mMMatrixHandle, 1, false, mModelMatrix, 0);
        checkGlError("Passing the Matrices in");
        
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, faceBufInd);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, 0);
        
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mNormalHandle);
        GLES20.glDisableVertexAttribArray(mTexCoorHandle);
        
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	public static void setCamPos(float camposx, float camposy, float camposz) {
		Draw.camposx = camposx;
		Draw.camposy = camposy;
		Draw.camposz = camposz;
	}



}
