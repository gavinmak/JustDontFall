package com.gavinmak;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ModelInstanceControllerRenderData;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ShortArray;

import java.awt.event.MouseAdapter;

public class Game extends ApplicationAdapter implements InputProcessor {

    private SpriteBatch batch;
    private PolygonSpriteBatch polyBatch;
    private BitmapFont font;

    private OrthographicCamera camera;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private Model path3d;
    private ModelInstance modelInstance;
    private Environment environment;

    private Texture charTexture;
    private Sprite charSprite;
    private Texture platformTexture;
    private PolygonSprite pathPolySprite;

    // game parameters
    private boolean alive = true;
    private int t = 0;

    // screen parameters
    private float screenWidth, screenHeight;

    // character parameters
    private float charSpriteWidth;
    private float posCharX, posCharY;

    // platform parameters
    private float widthPosVarMax = 0.33f;
    private Vector2[] drawnPoints;
    private Vector2[] cp;

    private int k = 200;
    private int numPointsPath = 8;
    private float pointsYDiff;
    private float pathWidth;

    private Vector2 measureAngle;
    private Vector2[] pointAngles;
    private float[][] left, right;

    @Override
    public void create() {
        // initialize parameters
        batch = new SpriteBatch();
        polyBatch = new PolygonSpriteBatch();

        font = new BitmapFont();

        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        camera = new OrthographicCamera(screenWidth, screenHeight);
        camera.position.set(0f, 0f, 10f);
        camera.lookAt(0f, 0f, 0f);

        /*
        batch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        path3d = modelBuilder.*/

        posCharX = screenWidth / 2;
        posCharY = screenHeight * 7 / 8;

        pointsYDiff = screenHeight / 2.5f;
        pathWidth = screenWidth * 0.13f;

        drawnPoints = new Vector2[k];
        cp = new Vector2[numPointsPath];

        pointAngles = new Vector2[k];

        for (int p = -2; p < numPointsPath - 2; p++)
            cp[p + 2] = new Vector2(((float)Math.random() * 2 - 1) * calcPathVariation() * screenWidth + screenWidth / 2,
                    p * pointsYDiff);

        // initializes a spline which represents path
        CatmullRomSpline<Vector2> platformPath = new CatmullRomSpline<Vector2>(cp, false);
        for (int i = 0; i < k; i++)
        {
            drawnPoints[i] = new Vector2();
            platformPath.valueAt(drawnPoints[i], ((float)i)/((float)k-1));

            measureAngle = new Vector2();
            platformPath.derivativeAt(measureAngle, ((float)i)/((float)k-1));
            pointAngles[i] = measureAngle.rotate90(0).nor().scl(pathWidth);
        }

        left = new float[k][2];
        right = new float[k][2];

        calcPathAngle();

        charTexture = new Texture(Gdx.files.internal("character.png"));
        charSprite = new Sprite(charTexture);

        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0xFFFFFFFF); // DE is red, AD is green and BE is blue.
        pix.fill();

        platformTexture = new Texture(pix);

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render() {
        // clear screen to make black backdrop
        Gdx.gl.glClearColor(0.5f, 0f, 0.5f, 0.2f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);

        if (alive) {
            drawPlatform();
            drawChar();
            drawHUD();

            t++;
        }

        else {
            drawLose();
            drawPlatform();
            drawChar();
            drawHUD();
        }

    }

    private void drawChar() {
        batch.begin();
        charSprite.setCenter(posCharX, screenHeight - posCharY);
        charSprite.draw(batch);
        batch.end();
    }

    private void drawPlatform() {
        // if the point is below the top of the screen, add a new point
        // need to check the perpendiculars, too
        if(drawnPoints[k-1].y - calcSpeed() < screenHeight + pointsYDiff * 1)
            calcPlatform();

        calcPathAngle();

        // must register points as vertices
        float[] vert = new float[k * 2];

        // first vertex is bottom left
        vert[0] = left[0][0];
        vert[1] = left[0][1] - calcSpeed();

        // right side up
        for(int i = 2; i < k - 1; i = i + 2) {
            vert[i] = right[i][0];
            vert[i + 1] = right[i][1] - calcSpeed();
        }

        // left side down
        for(int i = 0; i < k - 1; i = i + 2) {
            vert[i + k] = left[k - 1 - i][0];
            vert[i + 1 + k] = left[k - 1 - i][1] - calcSpeed();
        }

        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        ShortArray triangleIndices = triangulator.computeTriangles(vert);
        PolygonRegion polygonRegion = new PolygonRegion(new TextureRegion(platformTexture),
                vert, triangleIndices.toArray());

        polyBatch.begin();

        pathPolySprite = new PolygonSprite(polygonRegion);
        pathPolySprite.draw(polyBatch);

        polyBatch.end();

        Intersector check = new Intersector();

        // checks if character inside polygon
        if (!check.isPointInPolygon(vert, 0, 2 * k, posCharX, screenHeight - posCharY))
            alive = false;

    }

    private void drawHUD() {

    }

    private float calcSpeed() { return (float)Math.pow(t, 1.6) * 0.13f; }

    private float calcPathVariation() { return Math.max(0.1f, Math.min(t * 0.005f, widthPosVarMax)); }

    private void calcPlatform() {
        // shift set of points down to update points
        for (int p = 0; p < cp.length - 1; p++)
            cp[p] = cp[p + 1];

        // generate a new point
        cp[cp.length - 1] = new Vector2(((float)Math.random() * 2 - 1) * calcPathVariation() * screenWidth + screenWidth / 2,
                cp[cp.length - 1].y + pointsYDiff);

        // recreate spline and reinitialize drawnPoints
        CatmullRomSpline<Vector2> platformPath = new CatmullRomSpline<Vector2>(cp, false);
        for (int i = 0; i < k; i++)
        {
            drawnPoints[i] = new Vector2();
            platformPath.valueAt(drawnPoints[i], ((float)i)/((float)k-1));

            measureAngle = new Vector2();
            platformPath.derivativeAt(measureAngle, ((float)i)/((float)k-1));

            // makes perpendicular, makes length of path width
            pointAngles[i] = measureAngle.rotate90(0).nor().scl(pathWidth);
        }
    }

    private void calcPathAngle() {
        // initialize left and right perpendicular points
        for(int i = 0; i < k; i++) {
            left[i][0] = drawnPoints[i].x + pointAngles[i].x;
            left[i][1] = drawnPoints[i].y + pointAngles[i].y;

            right[i][0] = drawnPoints[i].x - pointAngles[i].x;
            right[i][1] = drawnPoints[i].y - pointAngles[i].y;
        }
    }

    private void drawLose() {

    }

    @Override
    public void dispose() {
        charTexture.dispose();
        platformTexture.dispose();
        font.dispose();

        batch.dispose();
        polyBatch.dispose();
    }

    public boolean touchDown(int x, int y, int pointer, int button) {
        if(alive) {
            posCharX = x;
            posCharY = y;
        }
        return true;
    }

    public boolean touchUp(int x, int y, int pointer, int button) {
        if(alive) {
            posCharX = x;
            posCharY = y;
        }
        return true;
    }

    public boolean touchDragged(int x, int y, int pointer) {
        if(alive) {
            posCharX = x;
            posCharY = y;
        }
        return true;
    }

    public boolean keyDown (int keycode) {
        return false;
    }

    public boolean keyUp (int keycode) {
        return false;
    }

    public boolean keyTyped (char character) {
        return false;
    }

    public boolean mouseMoved (int x, int y) {
        return false;
    }

    public boolean scrolled (int amount) {
        return false;
    }
}
