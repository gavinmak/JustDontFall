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
import com.badlogic.gdx.graphics.g2d.Sprite;
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

import java.awt.Font;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleBy;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

public class Game extends ApplicationAdapter implements InputProcessor {

    private Stage stage;
    private Table table;

    private SpriteBatch batch;
    private PolygonSpriteBatch polyBatch;

    private Texture pathTexture;
    private Texture pathSide;
    private PolygonSprite pathPolySprite;

    // game parameters
    private boolean alive = true, paused = false;
    private float t = 0;
    private float dt = 1000;

    // screen parameters
    private float screenWidth, screenHeight;

    // character parameters
    private Player player;
    private boolean touchedChar = false;

    class Player extends Actor {
        public float posCharX, posCharY, radius;
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

        public void setPosition(float x, float y) {
            posCharX = x;
            posCharY = y;
        }
    }

    // platform parameters
    private float widthPosVarMax = 0.33f;
    private Vector2[] drawnPoints;
    private Vector2[] cp;

    private int k = 9 * 10;
    private int numPointsPath = 9;
    private float pointsYDiff;
    private float pathWidth;

    private Vector2 measureAngle;
    private Vector2[] pointAngles;
    private float[][] left, right;

    private float currentScore = 0;

    private BitmapFont scoreFont;
    private ShapeRenderer scoreRect;
    private float[] rectWidth;
    private GlyphLayout scoreGlyph;

    private BitmapFont textFont;
    private GlyphLayout textGlyph;

    // buttons
    private ImageButton pauseButton;

    @Override
    public void create() {
        // initialize parameters
        stage = new Stage();

        batch = new SpriteBatch();
        polyBatch = new PolygonSpriteBatch();

        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        pointsYDiff = screenHeight / 3.5f;
        pathWidth = screenWidth * 0.13f;

        drawnPoints = new Vector2[k];
        cp = new Vector2[numPointsPath];

        pointAngles = new Vector2[k];

        for (int p = -2; p < numPointsPath - 2; p++)
            cp[p + 2] = new Vector2(((float)Math.random() * 2 - 1) * calcPathVariation()
                    * screenWidth + screenWidth / 2, p * pointsYDiff);

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

        player = new Player();
        player.setPosition(screenWidth / 2, screenHeight * 1 / 8);
        player.radius = pathWidth *  0.66f;
        player.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if(alive && !paused && pointer == 0) {
                    if(Math.abs(player.posCharX - x) < player.radius * 2 &&
                            Math.abs(player.posCharY - y) < player.radius * 2) {
                        player.setPosition(x, y);
                        touchedChar = true;
                    }
                }
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if(alive && !paused && pointer < 1 && touchedChar) {
                    player.setPosition(x, y);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if(alive && !paused && pointer < 1 && touchedChar) {
                    player.setPosition(x, y);
                    touchedChar = false;
                }
            }
        });

        /*
        AlphaAction fadeIn = new AlphaAction();
        fadeIn.setAlpha(0);
        fadeIn.setDuration(2000);
        player.addAction(fadeIn);
        */

        stage.addActor(player);

        table = new Table();
        table.setFillParent(true);

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
        table.add(pauseButton).size(screenWidth / 8).expand().bottom().right();

        Pixmap pix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pix.setColor(0xFAFAFAFF); // DE is red, AD is green and BE is blue.
        pix.fill();
        pathTexture = new Texture(pix);
        pix.dispose();

        FreeTypeFontGenerator scoreGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/RobotoMono-Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter scoreParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        scoreParameter.size = (int)screenWidth / 8;
        scoreParameter.color = Color.BLACK;
        scoreFont = scoreGenerator.generateFont(scoreParameter);
        scoreGenerator.dispose();
        scoreRect = new ShapeRenderer();
        scoreGlyph = new GlyphLayout(scoreFont, "12345");

        FreeTypeFontGenerator textGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/KumarOne-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter textParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        textParameter.size = (int)screenWidth / 8;
        textParameter.color = Color.WHITE;
        textFont = scoreGenerator.generateFont(textParameter);
        textGenerator.dispose();
        textGlyph = new GlyphLayout(scoreFont, "paused");

        rectWidth = new float[6];
        for(int i = 0; i < 6; i++) {
            scoreGlyph = new GlyphLayout(scoreFont, "" + (int)Math.pow(10, i));
            rectWidth[i] = scoreGlyph.width;
        }

        pathSide = new Texture(Gdx.files.internal("pathside.png"));
        stage.addActor(table);
        table.setDebug(true);

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render() {
        // clear screen to make black backdrop
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));

        if (alive) {
            // fade in character
        }

        else {
            // slow down function
            dt = Math.max(0, dt - 30);
            pauseButton.getColor().a = 0;
            drawLose();
        }

        drawPlatform();
        drawHUD();
        stage.draw();

        if(!paused) {
            t += dt / 1000;
        }
        else if(alive) {
            drawPause();
        }

        if(dt > 0)
            currentScore = calcDistance() / 100000f;

        // need to make platform falling, deployment
        // change platform colors, 3d?
        // best score
    }

    private void drawHUD() {
        // draw back rectangle
        scoreRect.setColor(Color.LIGHT_GRAY);
        scoreRect.begin(ShapeRenderer.ShapeType.Filled);
        scoreRect.rect(0.4f * scoreFont.getCapHeight(), screenHeight - 1.9f * scoreFont.getCapHeight(),
                rectWidth[Math.max((int)Math.log10(currentScore + 1), 0)] + 0.5f * scoreFont.getCapHeight(), 1.5f * scoreFont.getCapHeight());

        // draw front rectangle
        scoreRect.setColor(Color.WHITE);
        scoreRect.rect(0.25f * scoreFont.getCapHeight(), screenHeight - 1.75f * scoreFont.getCapHeight(),
                rectWidth[Math.max((int)Math.log10(currentScore + 1), 0)] + 0.5f * scoreFont.getCapHeight(), 1.5f * scoreFont.getCapHeight());
        scoreRect.end();

        // draw score
        batch.begin();
        scoreFont.draw(batch, String.format("%.0f", currentScore), scoreFont.getCapHeight() / 2,
                screenHeight - scoreFont.getCapHeight() / 2);
        batch.end();
    }

    private void drawPlatform() {
        // if the point is below the top of the screen, add a new point
        if(drawnPoints[k-1].y - calcSpeed() < screenHeight + pointsYDiff * 1.4)
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
        PolygonRegion polygonRegion = new PolygonRegion(new TextureRegion(pathTexture),
                vert, triangleIndices.toArray());

        polyBatch.begin();
        // draw top to down vertices
        for(int i = k; i < 2 * k - 2; i = i + 2) {
            // draws if there is a change in x values
            if(vert[i + 2] - vert[i] > 0) {
                // left vertices
                polyBatch.draw(pathSide, vert[i], vert[i + 1] - pathSide.getHeight());
            } else {
                // right vertices
                polyBatch.draw(pathSide, vert[2 * k - i] - pathSide.getWidth(),
                        vert[2 * k - i + 1] - pathSide.getHeight());
            }
        }

        pathPolySprite = new PolygonSprite(polygonRegion);
        pathPolySprite.draw(polyBatch);
        polyBatch.end();

        Intersector check = new Intersector();

        // checks if character inside polygon
        if (!check.isPointInPolygon(vert, 0, 2 * k, player.posCharX, player.posCharY)) {
            //alive = false;
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

    private float calcSpeedDelay(float d) { return (float)Math.pow(t, 1.55) * 0.26f - d; }

    private float calcDistance() { return (float)Math.pow(t, 2.55) * 0.05f; }

    private float calcPathVariation() { return Math.max(0.05f, Math.min(t * 0.005f, widthPosVarMax)); }

    private void calcPlatform() {
        // shift set of points down to update points
        for (int p = 0; p < cp.length - 1; p++)
            cp[p] = cp[p + 1];

        // generate a new point
        cp[cp.length - 1] = new Vector2(((float)Math.random() * 2 - 1) * calcPathVariation()
                * screenWidth + screenWidth / 2, cp[cp.length - 1].y + pointsYDiff);

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

    private String createMessage() {
        if (currentScore < 5)
            return "You can do better by literally doing nothing.";
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
        textFont.dispose();;

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