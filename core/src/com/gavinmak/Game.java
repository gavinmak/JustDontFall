package com.gavinmak;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
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
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ShortArray;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleBy;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

public class Game extends ApplicationAdapter implements InputProcessor {

    public enum State {
        RUN,
        LOSE,
        MENU
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume()
    {
        paused = true;
    }

    public void setGameState(State s) {
        this.state = s;
    }

    // app parameters
    private State state = State.RUN;
    private static Stage stage;
    private static Table table;

    // game parameters
    private static boolean alive = true, paused = false;
    private static float t = 0;
    private static float dt = 1000;
    private static int currentScore = 0;

    // screen parameters
    private static int screenWidth, screenHeight;

    // character parameters
    private static Player player;
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
            // super.draw(batch, parentAlpha);
            setBounds(0, 0, screenWidth, screenHeight);
            batch.end();
            character.begin(ShapeRenderer.ShapeType.Filled);
            character.setColor(Color.RED);
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
    private static Vector2[] pointAngles;
    private static float[] pathVertices;
    private static float[] shadeVertices;
    private static float[][] left, right;
    private static EarClippingTriangulator triangulator;
    private static Intersector check;
    private static CatmullRomSpline<Vector2> platformPath;

    // text
    private static ShapeRenderer scoreRect;             // draw rectangle in background of scores
    private static int[] rectWidth;
    private static BitmapFont scoreFont;
    private static GlyphLayout scoreGlyph;
    private static BitmapFont textFont;
    private static GlyphLayout textGlyph;

    // sprites and textures
    private static SpriteBatch batch;
    private static PolygonSpriteBatch polyBatch;
    private static Texture pathTexture;
    private static Texture pathSide, pathSideImage;
    private static PolygonSprite pathPolySprite;
    private static PolygonSprite pathPolySpriteLeft;


    // buttons
    private static ImageButton pauseButton;

    @Override
    public void create() {
        // initialize parameters
        stage = new Stage();
        polyBatch = new PolygonSpriteBatch();
        batch = new SpriteBatch();

        screenHeight = Gdx.graphics.getHeight();
        screenWidth = Gdx.graphics.getWidth();
        pointsYDiff = (int)(screenHeight / 3.5);
        pathWidth = (int)(screenWidth * 0.13);

        drawnPoints = new Vector2[k];
        cp = new Vector2[numPointsPath];
        pointAngles = new Vector2[k];

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

        pathVertices = new float[k * 2];
        left = new float[k][2];
        right = new float[k][2];
        shadeVertices = new float[2 * k];

        triangulator = new EarClippingTriangulator();
        check = new Intersector();

        calcPathAngle();

        player = new Player();
        player.posCharX = screenWidth / 2;
        player.posCharY = screenHeight * 1 / 8;
        player.radius = (int)(pathWidth * 0.66);
        player.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if(alive && !paused && pointer == 0) {
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
                if(alive && !paused && pointer < 1 && touchedChar) {
                    player.posCharX = (int)x;
                    player.posCharY = (int)y;
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if(alive && !paused && pointer < 1 && touchedChar) {
                    player.posCharX = (int)x;
                    player.posCharY = (int)y;
                    touchedChar = false;
                }
            }
        });
        stage.addActor(player);

        /*
        AlphaAction fadeIn = new AlphaAction();
        fadeIn.setAlpha(0);
        fadeIn.setDuration(2000);
        player.addAction(fadeIn);
        */

        table = new Table();
        table.setFillParent(true);

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
        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0xFAFAFAFF);
        pix.fill();
        pathTexture = new Texture(pix);
        pix.dispose();

        // texture for side of path
        Pixmap pixSide = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixSide.setColor(0xBBBBBBFF);
        pixSide.fill();
        pathSide = new Texture(pixSide);
        pixSide.dispose();
        pathSideImage = new Texture(Gdx.files.internal("pathside.png"));

        // font for numbers
        FreeTypeFontGenerator scoreGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/VT323-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter scoreParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        scoreParameter.size = screenWidth / 6;
        scoreParameter.color = Color.BLACK;
        scoreFont = scoreGenerator.generateFont(scoreParameter);
        scoreGenerator.dispose();
        scoreRect = new ShapeRenderer();
        scoreGlyph = new GlyphLayout(scoreFont, "12345");

        // font for letters
        FreeTypeFontGenerator textGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/VT323-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter textParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        textParameter.size = screenWidth / 4;
        textParameter.color = Color.WHITE;
        textFont = scoreGenerator.generateFont(textParameter);
        textGenerator.dispose();
        textGlyph = new GlyphLayout(scoreFont, "paused");

        // pre-loads spacing for digits up to six spaces
        rectWidth = new int[6];
        for(int i = 0; i < 6; i++) {
            scoreGlyph = new GlyphLayout(scoreFont, "" + (int)Math.pow(10, i));
            rectWidth[i] = (int)scoreGlyph.width;
        }

        stage.addActor(table);
        table.setDebug(true);

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render() {
        // clear screen to make black backdrop
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));

        switch (state) {
            case RUN:
                if (alive) {
                    // fade in character

                } else {
                    // slow down function
                    dt = Math.max(0, dt - 20);
                    pauseButton.getColor().a = 0;
                    drawLose();
                }
                drawBackground();
                drawPlatform();
                drawHUD();
                stage.draw();

                if (!paused) {
                    t += dt / 1000;
                } else if (alive) {
                    drawPause();
                }

                if (dt > 0)
                    currentScore = (int) calcDistance() / 100000;

                // need to make platform falling, deployment
                // change platform colors
                // best score
                break;

            case LOSE:

                break;

            case MENU:

                break;
        }
    }

    private void drawBackground() {
        
    }

    private void drawHUD() {
        // draw back rectangle
        scoreRect.setColor(Color.LIGHT_GRAY);
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
        scoreFont.draw(batch, "" + currentScore, scoreFont.getCapHeight() * 0.7f,
                screenHeight - scoreFont.getCapHeight() * 0.7f);
        batch.end();
    }

    private void drawPlatform() {
        // if the point is below the top of the screen, add a new point
        if(drawnPoints[k-1].y - calcSpeed() < screenHeight + pointsYDiff * 1.4)
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
                polyBatch.draw(pathSideImage, pathVertices[i], pathVertices[i + 1] - pathSideImage.getHeight());
            } else {
                polyBatch.draw(pathSideImage, pathVertices[2 * k - i] - pathSideImage.getWidth(),
                        pathVertices[2 * k - i + 1] - pathSideImage.getHeight());
            }
        }

        // draw actual path
        pathPolySprite = new PolygonSprite(polygonRegion);
        pathPolySprite.draw(polyBatch);

        polyBatch.end();

        // checks if character inside polygon
        if (!check.isPointInPolygon(pathVertices, 0, 2 * k, player.posCharX, player.posCharY)) {
            alive = false;
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

    private void drawLose() {

    }

    private float calcSpeed() { return (float)Math.pow(t, 1.55) * 0.25f; }

    private float calcDistance() { return (float)Math.pow(t, 2.55) * 0.05f; }

    private float calcPathVariation() { return Math.max(0.05f, Math.min(t * 0.005f, widthPosVarMax)); }


    private void calcPlatform() {
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

    private String createMessage() {
        if (currentScore < 5)
            return "Come on now.";
        if (currentScore < 100)
            return "Baby steps.";
        if (currentScore < 200)
            return "Eh.";
        if (currentScore == 420)
            return "420 blaze it";
        if (currentScore < 500)
            return "You're getting there. Kinda.";
        if (currentScore < 1000)
            return "Good, not great.";
        if (currentScore < 2000)
            return "Okay, I see you.";
        if (currentScore < 3000)
            return "You're good, huh.";
        if (currentScore < 4000)
            return "MLG? Probably.";
        else
            return "EZ.";
    }

    public void resize (int width, int height) {
        stage.getViewport().update(width, height, true);
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

    public boolean touchDown(int x, int y, int pointer, int button) {
        return true;
    }

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