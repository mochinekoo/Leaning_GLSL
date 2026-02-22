package mochineko.learning_glsl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11C.GL_FALSE;
import static org.lwjgl.opengl.GL11C.glDrawArrays;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * シェーダのメイン関数。
 * @apiNote （コメント追加や直せそうなところを除き）　ほとんどがchatgptに頼ってます..ご了承ください。
 */
public class Main {

    private long window; //ウインドウハンドル
    private int programId; //プログラムID
    private int vao;
    private int offsetLocation;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        init();
        loop();
        glfwTerminate();
    }

    private void init() {
        if (!glfwInit())
            throw new IllegalStateException("GLFWの初期化に失敗したよお");

        glfwWindowHint(GLFW_SAMPLES, 4); // アンチエイリアス

        window = glfwCreateWindow(800, 600, "Rectangle", NULL, NULL); //ウインドウを作る関数
        glfwMakeContextCurrent(window);
        glfwShowWindow(window); //ウインドウを表示する関数

        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);

        String vertexCode = loadShader("/vertex.glsl"); //頂点シェーダ
        String fragmentCode = loadShader("/fragment.glsl"); //フラグメントシェーダー

        int vertexShader = glCreateShader(GL_VERTEX_SHADER); //頂点シェーダを作る関数
        glShaderSource(vertexShader, vertexCode);
        glCompileShader(vertexShader); //コンパイル
        checkCompile(vertexShader);

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER); //フラグメントシェーダを作る関数
        glShaderSource(fragmentShader, fragmentCode);
        glCompileShader(fragmentShader); //コンパイル
        checkCompile(fragmentShader);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException(glGetProgramInfoLog(programId));

        //メモリを解放
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        //頂点シェーダの行列
        float[] vertices = {
                -0.5f,  0.5f, 0.0f, // 左上
                -0.5f, -0.5f, 0.0f, // 左下
                0.5f, -0.5f, 0.0f, // 右下
                0.5f,  0.5f, 0.0f  // 右上
        };

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);

        offsetLocation = glGetUniformLocation(programId, "offset");
    }

    /**
     * ゲームループを実行する関数
     */
    private void loop() {
        float time;
        float x = 0.0f, y = 0.0f;
        float vec = 0.01f;
        while (!glfwWindowShouldClose(window)) {
            time = (float) glfwGetTime();

            glClear(GL_COLOR_BUFFER_BIT);

            if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) { //左キー
               x -= vec;
            }
            if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) { //右キー
                x += vec;
            }
            if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) { //上キー
                y += vec;
            }
            if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) { //下キー
                y -= vec;
            }

            glUseProgram(programId);

            //float x = (float) Math.sin(time) * 0.5f;
            glUniform2f(offsetLocation, x, y);

            glBindVertexArray(vao);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    /**
     * 特定パスにあるシェーダを、文字列として返す関数
     * @param path 取得したいパス
     * @return コードを文字列にしたもの
     * @apiNote chatgptが書いたコードです
     */
    private String loadShader(String path) {
        InputStream in = Main.class.getResourceAsStream(path);
        if (in == null)
            throw new RuntimeException("シェーダが見つからなかったよ: " + path);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void checkCompile(int shader) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException(glGetShaderInfoLog(shader));
    }
}
