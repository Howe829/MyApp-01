package com.example.fadin.myapp.Util.face0;

import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.example.fadin.myapp.Util.face0.CameraController;
import com.example.fadin.myapp.Util.face0.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class RenderScreen {
    private final FloatBuffer mNormalVtxBuf = GlUtil.createVertexBuffer();  //顶点坐标
    private final FloatBuffer mNormalVtxBufImage = GlUtil.createVertexBufferImage();  //顶点坐标
    private FloatBuffer mCameraTexCoordBuffer; //纹理坐标,根据窗口大小和图像大小生成
    private final float[] mPosMtx = GlUtil.createIdentityMtx();

    private int mFboTexId;

    private int mProgram = -1;
    private int maPositionHandle = -1;
    private int maTexCoordHandle = -1;
    private int muPosMtxHandle = -1;
    private int muTexMtxHandle = -1;
    private int muSamplerHandle = -1;

    private int mScreenW = -1;
    private int mScreenH = -1;

    private boolean mirrorImage;//是否开启镜像

    public RenderScreen(int id) {
        initGL();

        mFboTexId = id;
        mirrorImage = false;
    }

    public void setSreenSize(int width, int height) {
        mScreenW = width;
        mScreenH = height;

        initCameraTexCoordBuffer();
    }

    public void setTextureId(int textureId) {
        //摄像头纹理copy
        mFboTexId = textureId;
    }

    private void initCameraTexCoordBuffer() {
        int cameraWidth, cameraHeight;
        Camera.Size size = CameraController.getInstance().getmPreviewSize();
        int width = size.width;
        int height = size.height;
        if(CameraController.getInstance().isLandscape()) {
            cameraWidth = Math.max(width, height);
            cameraHeight = Math.min(width, height);
        } else {
            cameraWidth = Math.min(width, height);
            cameraHeight = Math.max(width, height);
        }

        float hRatio = mScreenW / ((float)cameraWidth);
        float vRatio = mScreenH / ((float)cameraHeight);

        float ratio;
        if(hRatio > vRatio) {
            ratio = mScreenH / (cameraHeight * hRatio);
            final float vtx[] = {
                    //UV
                    0f, 0.5f + ratio/2,
                    0f, 0.5f - ratio/2,
                    1f, 0.5f + ratio/2,
                    1f, 0.5f - ratio/2,
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
            bb.order(ByteOrder.nativeOrder());
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
        } else {
            ratio = mScreenW/ (cameraWidth * vRatio);
            //横排显示
            //            final float vtx[] = {
            //                    //UV
            //                    0.5f - ratio/2, 1f,
            //                    0.5f - ratio/2, 0f,
            //                    0.5f + ratio/2, 1f,
            //                    0.5f + ratio/2, 0f,
            //            };
            //竖屏显示 放大
            //            final float vtx[] = {
            //                    //UV
            //                    0.5f - ratio/2, 1f,
            //                    0.5f + ratio/2, 1f,
            //                    0.5f - ratio/2, 0f,
            //                    0.5f + ratio/2, 0f,
            //            };
            //竖屏 不放大
            final float vtx[] = {
                    //UV
                    0f, 0.5f + ratio/2,
                    1f, 0.5f + ratio/2,
                    0f, 0.5f - ratio/2,
                    1f, 0.5f - ratio/2,
            };
            ByteBuffer bb = ByteBuffer.allocateDirect(4 * vtx.length);
            bb.order(ByteOrder.nativeOrder());
            mCameraTexCoordBuffer = bb.asFloatBuffer();
            mCameraTexCoordBuffer.put(vtx);
            mCameraTexCoordBuffer.position(0);
        }
    }

    public void draw(final float[] tex_mtx) {
        if (mScreenW <= 0 || mScreenH <= 0) {
            return;
        }

        GlUtil.checkGlError("draw_S");

        //设置视口大小
        GLES20.glViewport(0, 0, mScreenW, mScreenH);
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        //设置定点坐标
        if(mirrorImage){
            mNormalVtxBuf.position(0);
            GLES20.glVertexAttribPointer(maPositionHandle,
                    3, GLES20.GL_FLOAT, false, 4 * 3, mNormalVtxBuf);
        }else{
            mNormalVtxBufImage.position(0);
            GLES20.glVertexAttribPointer(maPositionHandle,
                    3, GLES20.GL_FLOAT, false, 4 * 3, mNormalVtxBufImage);
        }
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        //设置纹理坐标
        mCameraTexCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(maTexCoordHandle,
                2, GLES20.GL_FLOAT, false, 4 * 2, mCameraTexCoordBuffer);
        GLES20.glEnableVertexAttribArray(maTexCoordHandle);

        //设置变换矩阵
        if(muPosMtxHandle>= 0)
            GLES20.glUniformMatrix4fv(muPosMtxHandle, 1, false, mPosMtx, 0);

        if(muTexMtxHandle>= 0)
            GLES20.glUniformMatrix4fv(muTexMtxHandle, 1, false, tex_mtx, 0);

        //绑定纹理，将纹理渲染
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mFboTexId);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GlUtil.checkGlError("draw_E");
    }

    private void initGL() {
        GlUtil.checkGlError("initGL_S");

        final String vertexShader =
                //
                "attribute vec4 position;\n" +
                        "attribute vec4 inputTextureCoordinate;\n" +
                        "uniform   mat4 uPosMtx;\n" +
                        "varying   vec2 textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_Position = uPosMtx * position;\n" +
                        "  textureCoordinate   = inputTextureCoordinate.xy;\n" +
                        "}\n";
        final String fragmentShader =
                //
                "precision mediump float;\n" +
                        "uniform sampler2D uSampler;\n" +
                        "varying vec2  textureCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(uSampler, textureCoordinate);\n" +
                        "}\n";


        String SHARDE_NULL_VERTEX = "attribute vec4 position;\n" +
                "attribute vec4 inputTextureCoordinate;\n" +
                "\n" +
                "uniform   mat4 uPosMtx;\n" +
                "uniform   mat4 uTexMtx;\n" +
                "varying   vec2 textureCoordinate;\n" +
                "void main() {\n" +
                "  gl_Position = uPosMtx * position;\n" +
                "  textureCoordinate   = (uTexMtx * inputTextureCoordinate).xy;\n" +
                "}";

        String SHARDE_NULL_FRAGMENT = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform samplerExternalOES uSampler;\n" +
                "void main() {\n" +
                "    vec4 tc = texture2D(uSampler, textureCoordinate);\n" +
                "    gl_FragColor = vec4(tc.r, tc.g, tc.b, 1.0);\n" +
                "}";

        //        mProgram = GlUtil.createProgram(vertexShader, fragmentShader);
        mProgram = GlUtil.createProgram(SHARDE_NULL_VERTEX, SHARDE_NULL_FRAGMENT);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");
        maTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        muPosMtxHandle = GLES20.glGetUniformLocation(mProgram, "uPosMtx");
        muTexMtxHandle = GLES20.glGetUniformLocation(mProgram, "uTexMtx");
        muSamplerHandle = GLES20.glGetUniformLocation(mProgram, "uSampler");

        GlUtil.checkGlError("initGL_E");
    }
}
