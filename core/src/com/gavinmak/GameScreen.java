package com.gavinmak;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.ScaleByAction;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ShortArray;
import com.badlogic.gdx.utils.Timer;

/**
 * Created by gavin on 12/24/16.
 */

public class GameScreen implements Screen, InputProcessor{
    final Fall fall;

    // app parameters
    private static Stage stage;
    private static Table table, endTable;

    // game parameters
    private static boolean alive = true, paused = false;
    private static float t = 0, dt = 1000;
    private static int currentScore = 0, bestScore = 0;
    private ShapeRenderer fade, background;
    private static float transitionDelta = 0;

    // screen parameters
    private static int screenWidth, screenHeight;

    // character parameters
    private static Player player;
    private ScaleByAction playerDeath;
    private static boolean touchedChar = false;

    class Player extends Actor {
        public int posCharX, posCharY, radius;
        private Vector2[] velChar;
        private ShapeRenderer character;

        public Player() {
            character = new ShapeRenderer();
            velChar = new Vector2[2];
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            setBounds(0, 0, screenWidth, screenHeight);
            batch.end();
            character.begin(ShapeRenderer.ShapeType.Filled);
            character.setColor(accentColor);
            character.circle(posCharX, posCharY, radius);
            character.end();
            batch.begin();
        }
    }

    // platform parameters
    private static final float widthPosVarMax = 0.33f;  // amount the path can move left to right
    private static Vector2[] drawnPoints;               // points drawn between control points
    private static Vector2[] cp;                        // control points for spline

    private static final int k = 9 * 20;                // number of points drawn in spine
    private static final int numPointsPath = 9;         // number of control points in spline
    private static int pointsYDiff;                     // vertical spacing between each control points
    private static int pathWidth;
    private static final int pathHeight = 600;          // side path shade height

    private static Vector2 measureAngle;                // stores derivatives at each point in spline
    private static Vector2[] pointAngles;               // angles of each derivative
    private static float[] pathVertices, shadeVertices;
    private static float[][] left, right;               // left and right vertices for the path
    private static EarClippingTriangulator triangulator;
    private static Intersector check;
    private static CatmullRomSpline<Vector2> platformPath;

    private Color primaryColor, secondaryColor, accentColor, backgroundColor;
    private Pixmap pix;

    // text
    FreeTypeFontGenerator fontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/VT323-Regular.ttf"));
    FreeTypeFontGenerator.FreeTypeFontParameter fontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
    private static ShapeRenderer scoreRect;             // draw rectangle in background of scores
    private static int[] rectWidth;
    private static BitmapFont scoreFont, bestScoreFont;
    private static GlyphLayout scoreGlyph;
    private static BitmapFont textFont;
    private static GlyphLayout textGlyph;

    // sprites and textures
    private static SpriteBatch batch;
    private static PolygonSpriteBatch polyBatch;
    private static Texture pathTexture, pathSide;
    private static PolygonSprite pathPolySprite, pathPolySpriteLeft;

    // buttons
    private static ImageButton pauseButton;
    private static TextButton restartButton, quitButton;
    private static TextButton.TextButtonStyle endButtonStyle;
    Timer timer;
    private static Timer.Task clickTimerRestart, clickTimerQuit;


    public GameScreen(final Fall game) {
        this.fall = game;

        // initialize parameters
        stage = new Stage();
        polyBatch = new PolygonSpriteBatch();
        batch = new SpriteBatch();

        fade = new ShapeRenderer();
        background = new ShapeRenderer();

        screenHeight = Gdx.graphics.getHeight();
        screenWidth = Gdx.graphics.getWidth();
        pointsYDiff = (int)(screenHeight / 3.5);
        pathWidth = (int)(screenWidth * 0.13);

        primaryColor = new Color();
        secondaryColor = new Color();
        accentColor = new Color();
        backgroundColor = new Color();
        getColor(currentScore);

        // loads previous best score
        Preferences prefs = Gdx.app.getPreferences("justdontfall");
        bestScore = prefs.getInteger("hi", 0);

        drawnPoints = new Vector2[k];
        cp = new Vector2[numPointsPath];
        pointAngles = new Vector2[k];

        // initializes beginning path control points, with little to no variation
        for (int p = -2; p < numPointsPath - 2; p++)
            cp[p + 2] = new Vector2(((float)Math.random() * 2 - 1) * calcPathVariation()
                    * screenWidth + screenWidth / 2, p * pointsYDiff);

        // initializes a spline which represents path
        platformPath = new CatmullRomSpline<Vector2>(cp, false);
        for (int i = 0; i < k; i++)
        {
            drawnPoints[i] = new Vector2();
            platformPath.valueAt(drawnPoints[i], ((float)i)/((float)k-1));
            measureAngle = new Vector2();
            platformPath.derivativeAt(measureAngle, ((float)i)/((float)k-1));
            pointAngles[i] = measureAngle.rotate90(0).nor().scl(pathWidth);
        }

        pathVertices = new float[k * 2];
        left = new float[k][2];
        right = new float[k][2];
        shadeVertices = new float[k * 2];
        triangulator = new EarClippingTriangulator();
        check = new Intersector();

        calcPathAngle();

        player = new Player();
        player.posCharX = screenWidth / 2;
        player.posCharY = screenHeight / 2;
        player.radius = (int)(pathWidth * 0.66);
        player.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if(alive && !paused) {
                    if(Math.abs(player.posCharX - x) < player.radius * 3 &&
                            Math.abs(player.posCharY - y) < player.radius * 3) {
                        player.posCharX = (int)x;
                        player.posCharY = (int)y;
                        touchedChar = true;
                    }
                }
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if(alive && !paused && touchedChar) {
                    player.posCharX = (int)x;
                    player.posCharY = (int)y;
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if(alive && !paused && touchedChar) {
                    player.posCharX = (int)x;
                    player.posCharY = (int)y;
                    touchedChar = false;
                }
            }
        });
        playerDeath = new ScaleByAction();
        playerDeath.setAmount(-10);
        stage.addActor(player);

        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // images for the play/pause button
        TextureRegionDrawable pauseUp = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("pause.png"))));
        TextureRegionDrawable pauseDown = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("play.png"))));

        pauseButton = new ImageButton(pauseUp, pauseUp, pauseDown);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                paused = !paused;
            }
        });
        stage.addActor(pauseButton);
        table.add(pauseButton).size(screenWidth / 6).expand().bottom().right();

        // texture for path
        pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(primaryColor);
        pix.fill();
        pathTexture = new Texture(pix);

        // texture for side of path
        pix.setColor(secondaryColor);
        pix.fill();
        pathSide = new Texture(pix);

        // font for numbers
        fontParameter.size = screenWidth / 6;
        fontParameter.color = Color.valueOf("#212121");
        scoreFont = fontGenerator.generateFont(fontParameter);
        scoreRect = new ShapeRenderer();
        scoreGlyph = new GlyphLayout(scoreFont, "12345");

        fontParameter.color = Color.valueOf("#FFD700");
        bestScoreFont = fontGenerator.generateFont(fontParameter);


        // font for letters
        fontParameter.size = screenWidth / 4;
        fontParameter.color = Color.WHITE;
        textFont = fontGenerator.generateFont(fontParameter);
        textGlyph = new GlyphLayout(scoreFont, "paused");

        // pre-loads spacing for digits up to six spaces
        rectWidth = new int[6];
        for(int i = 0; i < 6; i++) {
            scoreGlyph = new GlyphLayout(scoreFont, "" + (int)Math.pow(10, i));
            rectWidth[i] = (int)scoreGlyph.width;
        }

        endTable = new Table();
        endTable.setFillParent(true);
        stage.addActor(endTable);

        endButtonStyle = new TextButton.TextButtonStyle();
        endButtonStyle.font = textFont;
        endButtonStyle.downFontColor = accentColor;
        endButtonStyle.fontColor = Color.WHITE;

        timer = new Timer();

        clickTimerRestart = new Timer.Task() {
            @Override
            public void run() {
                resetGame();
            }
        };

        clickTimerQuit = new Timer.Task() {
            @Override
            public void run() {
                resetGame();
                fall.setScreen(new MainMenuScreen(fall));
            }
        };

        restartButton = new TextButton("restart", endButtonStyle);
        restartButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                timer.scheduleTask(clickTimerRestart, 0.2f);
                return true;
            }
        });
        endTable.add(restartButton).expand().padTop(screenHeight / 8).row();

        quitButton = new TextButton("give up", endButtonStyle);
        quitButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                timer.scheduleTask(clickTimerQuit, 0.3f);
                return true;
            }
        });
        endTable.add(quitButton).expand().padBottom(screenHeight / 8);

        endTable.setVisible(false);

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        // clear screen
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT |
                (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));

        drawBackground();
        drawPlatform();

        // fade in black screen
        Gdx.graphics.getGL20().glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        fade.begin(ShapeRenderer.ShapeType.Filled);
        if(alive)
            fade.setColor(new Color(0, 0, 0, 1 - transitionDelta / 100f));
        else
            fade.setColor(new Color(0, 0, 0, transitionDelta / 100f));
        fade.rect(0, 0, screenWidth, screenHeight);
        fade.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        drawHUD();
        stage.draw();

        if(!paused) {
            t += dt / 1000;
        } else if(alive) {
            drawPause();
        }

        if(alive) {
            table.setVisible(true);
            endTable.setVisible(false);
        }
        else {
            table.setVisible(false);
            endTable.setVisible(true);
            pauseButton.setVisible(false);

            dt = Math.max(0, dt - 30);
        }

        if(dt > 0)
            currentScore = (int) calcDistance() / 1000;

        // keeps a steady track for animations
        transitionDelta += 1;
    }

    private void drawBackground() {
        background.begin(ShapeRenderer.ShapeType.Filled);
        background.setColor(backgroundColor);
        background.rect(0, 0, screenWidth, screenHeight);
        background.end();
    }

    private void drawHUD() {
        // draw back rectangle
        scoreRect.setColor(accentColor);
        scoreRect.begin(ShapeRenderer.ShapeType.Filled);
        scoreRect.rect(0.6f * scoreFont.getCapHeight(), screenHeight - 2.1f * scoreFont.getCapHeight(),
                rectWidth[Math.max((int)Math.log10(currentScore), 0)] + 0.5f * scoreFont.getCapHeight(), 1.5f * scoreFont.getCapHeight());

        // draw front rectangle
        scoreRect.setColor(Color.WHITE);
        scoreRect.rect(0.45f * scoreFont.getCapHeight(), screenHeight - 1.95f * scoreFont.getCapHeight(),
                rectWidth[Math.max((int)Math.log10(currentScore), 0)] + 0.5f * scoreFont.getCapHeight(), 1.5f * scoreFont.getCapHeight());
        scoreRect.end();

        // draw score
        batch.begin();
        // if the current score is better, make it gold colored
        if(currentScore > bestScore) {
            bestScoreFont.draw(batch, "" + currentScore, scoreFont.getCapHeight() * 0.7f,
                    screenHeight - scoreFont.getCapHeight() * 0.7f);
        } else {
            scoreFont.draw(batch, "" + currentScore, scoreFont.getCapHeight() * 0.7f,
                    screenHeight - scoreFont.getCapHeight() * 0.7f);
        }
        batch.end();
    }

    private void drawPlatform() {
        // if the point is below the top of the screen, add a new point
        if(drawnPoints[k-1].y - calcSpeed() < screenHeight + pointsYDiff * 1.35)
            calcPlatform();

        calcPathAngle();

        // registering path vertices
        // first vertex is bottom left
        pathVertices[0] = left[0][0];
        pathVertices[1] = left[0][1] - calcSpeed();

        // vertices for drawing path
        for(int i = 2; i < k - 1; i += 2) {
            // right side up
            pathVertices[i] = right[i][0];
            pathVertices[i + 1] = right[i][1] - calcSpeed();
        }

        for(int i = 0; i < k - 1; i += 2) {
            // left side down
            pathVertices[i + k] = left[k - 1 - i][0];
            pathVertices[i + 1 + k] = left[k - 1 - i][1] - calcSpeed();
        }

        ShortArray triangleIndices = triangulator.computeTriangles(pathVertices);
        PolygonRegion polygonRegion = new PolygonRegion(new TextureRegion(pathTexture),
                pathVertices, triangleIndices.toArray());

        // create an identical path shifted down by pathHeight
        for (int i = 0; i < 2 * k; i += 2) {
            shadeVertices[i] = pathVertices[i];
            shadeVertices[i + 1] = pathVertices[i + 1] - pathHeight;
        }

        ShortArray triangleLeft = triangulator.computeTriangles(shadeVertices);
        PolygonRegion polygonRegionLeft = new PolygonRegion(new TextureRegion(pathSide),
                shadeVertices, triangleLeft.toArray());

        polyBatch.begin();

        // draw path shade
        pathPolySpriteLeft = new PolygonSprite(polygonRegionLeft);
        pathPolySpriteLeft.draw(polyBatch);

        // draw rectangles to fill space between path and shifted down path
        for(int i = k; i < 2 * k - 2; i += 2) {
            // draws if there is a change in x values
            if(pathVertices[i + 2] - pathVertices[i] > 0) {
                polyBatch.draw(pathSide, pathVertices[i], pathVertices[i + 1] -  pathHeight,
                        200, pathHeight);
            } else {
                polyBatch.draw(pathSide, pathVertices[2 * k - i] - 200, pathVertices[2 * k - i + 1] - pathHeight,
                        200, pathHeight);
            }
        }

        // draw actual path
        pathPolySprite = new PolygonSprite(polygonRegion);
        pathPolySprite.draw(polyBatch);

        polyBatch.end();

        // checks if character inside polygon, goes through only once
        if(!check.isPointInPolygon(pathVertices, 0, 2 * k, player.posCharX, player.posCharY) && alive) {
            alive = false;
            transitionDelta = 0;
        }
    }

    private void drawPause() {
        Gdx.graphics.getGL20().glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        scoreRect.begin(ShapeRenderer.ShapeType.Filled);
        scoreRect.setColor(new Color(0, 0, 0, 0.6f));
        scoreRect.rect(0, 0, screenWidth, screenHeight);
        scoreRect.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        textGlyph.setText(textFont, "paused");
        textFont.draw(batch, "paused", screenWidth / 2 - textGlyph.width / 2,
                screenHeight / 2 + textGlyph.height / 2);
        batch.end();
    }

    // called to reset game to initial parameters
    private void resetGame() {
        if(currentScore > bestScore) {
            Preferences prefs = Gdx.app.getPreferences("justdontfall");
            prefs.putInteger("hi", currentScore);
            prefs.flush();
            bestScore = currentScore;
        }

        currentScore = 0;
        changeColors();
        t = 0;
        transitionDelta = 0;
        dt = 1000;
        player.posCharY = screenHeight / 2;
        player.posCharX = screenWidth / 2;
        alive = true;
        paused = false;
        touchedChar = false;
        pauseButton.setVisible(true);
        pauseButton.setChecked(false);
        endTable.setVisible(false);
        table.setVisible(true);

        // initializes beginning path control points, with little to no variation
        for (int p = -2; p < numPointsPath - 2; p++)
            cp[p + 2] = new Vector2(((float)Math.random() * 2 - 1) * calcPathVariation()
                    * screenWidth + screenWidth / 2, p * pointsYDiff);

        // initializes a spline which represents path
        // explained in further detail in calcPlatform()
        platformPath = new CatmullRomSpline<Vector2>(cp, false);
        for (int i = 0; i < k; i++)
        {
            drawnPoints[i] = new Vector2();
            platformPath.valueAt(drawnPoints[i], ((float)i)/((float)k-1));
            measureAngle = new Vector2();
            platformPath.derivativeAt(measureAngle, ((float)i)/((float)k-1));
            pointAngles[i] = measureAngle.rotate90(0).nor().scl(pathWidth);
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT |
                (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));

        drawBackground();
        drawPlatform();
        drawHUD();

        stage.draw();
    }

    // returns the current speed
    private float calcSpeed() { return (float)Math.pow(t, 1.56) * 0.25f; }

    // returns the current score, distance
    private float calcDistance() { return (float)Math.pow(t, 1.5) * 10f; }

    // returns the amount the path should vary from side to side
    private float calcPathVariation() { return Math.max(0.05f, Math.min(t * 0.005f, widthPosVarMax)); }


    private void calcPlatform() {
        changeColors();

        /*
        adds a point to the platform at the top of the screen, out of the viewport
        and fills array of angles such to maintain a constant path width
        */

        // shift set of points down to update points
        for (int p = 0; p < cp.length - 1; p++)
            cp[p] = cp[p + 1];

        // generate a new point at top
        cp[cp.length - 1] = new Vector2(((float)Math.random() * 2 - 1) * calcPathVariation()
                * screenWidth + screenWidth / 2, cp[cp.length - 1].y + pointsYDiff);

        // recreate spline and reinitialize drawnPoints
        for (int i = 0; i < k; i++)
        {
            // fill array with points on spline
            drawnPoints[i] = new Vector2();
            platformPath.valueAt(drawnPoints[i], ((float)i)/((float)k-1));

            // fill angle array with angles to be drawn such that the path width is constant
            measureAngle = new Vector2();
            platformPath.derivativeAt(measureAngle, ((float)i)/((float)k-1));
            pointAngles[i] = measureAngle.rotate90(0).nor().scl(pathWidth);
        }
    }

    /* calculates the perpendicular angle of each point on the path */
    private void calcPathAngle() {
        // initialize left and right perpendicular points
        for(int i = 0; i < k; i++) {
            // points to left and right are the middle of the path plus or minus the angles
            // because the vectors are equal length, path width is preserved
            left[i][0] = (int)(drawnPoints[i].x + pointAngles[i].x);
            left[i][1] = (int)(drawnPoints[i].y + pointAngles[i].y);
            right[i][0] = (int)(drawnPoints[i].x - pointAngles[i].x);
            right[i][1] = (int)(drawnPoints[i].y - pointAngles[i].y);
        }
    }

    /* updates the colors */
    private void changeColors() {
        getColor(currentScore);
        pix.setColor(primaryColor);
        pix.fill();
        pathTexture = new Texture(pix);

        pix.setColor(secondaryColor);
        pix.fill();
        pathSide = new Texture(pix);
    }

    private void getColor(int score) {
        if(score < 1000) {
            // white / black
            primaryColor.set(Color.valueOf("#FAFAFA"));
            secondaryColor.set(Color.valueOf("#9E9E9E"));
            accentColor.set(Color.valueOf("#555555"));
            backgroundColor.set(Color.valueOf("#000000"));
        }

        else if(score < 2000) {
            // yellow
            primaryColor.set(Color.valueOf("#FFEB3B"));
            secondaryColor.set(Color.valueOf("#FBC02D"));
            accentColor.set(Color.valueOf("#ff3b62"));
            backgroundColor.set(Color.valueOf("#3b89ff"));
        }

        else if(score < 3000) {
            // blue
            primaryColor.set(Color.valueOf("#42A5F5"));
            secondaryColor.set(Color.valueOf("#1565C0"));
            accentColor.set(Color.valueOf("#FF42A7"));
            backgroundColor.set(Color.valueOf("#A5F542"));
        }

        else if(score < 4000) {
            // purple
            primaryColor.set(Color.valueOf("#AB47BC"));
            secondaryColor.set(Color.valueOf("#6A1B9A"));
            accentColor.set(Color.valueOf("#FFE65b"));
            backgroundColor.set(Color.valueOf("#5250EF"));
        }

        else if(score < 5000) {
            // orange
            primaryColor.set(Color.valueOf("#FF9800"));
            secondaryColor.set(Color.valueOf("#F57C00"));
            accentColor.set(Color.valueOf("#02b042"));
            backgroundColor.set(Color.valueOf("#b80090"));
        }

        else if(score < 6000) {
            // red
            primaryColor.set(Color.valueOf("#EF5350"));
            secondaryColor.set(Color.valueOf("#C62828"));
            accentColor.set(Color.valueOf("#53FF53"));
            backgroundColor.set(Color.valueOf("#5250EF"));
        }

        else {
            // black
            primaryColor.set(Color.valueOf("#424242"));
            secondaryColor.set(Color.valueOf("#000000"));
            accentColor.set(Color.valueOf("#e0e0e0"));
            backgroundColor.set(Color.valueOf("#FFFFFF"));
        }

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        pathTexture.dispose();
        pathSide.dispose();
        scoreFont.dispose();
        textFont.dispose();
        scoreRect.dispose();
        stage.dispose();
        batch.dispose();
        polyBatch.dispose();
    }

    @Override
    public void pause() {
        paused = true;
        pauseButton.setChecked(true);
    }

    @Override
    public void resume()
    {
        paused = true;
        pauseButton.setChecked(true);
    }

    public boolean touchDown(int x, int y, int pointer, int button) { return true; }

    public boolean touchUp(int x, int y, int pointer, int button) {
        return true;
    }

    public boolean touchDragged(int x, int y, int pointer) {
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