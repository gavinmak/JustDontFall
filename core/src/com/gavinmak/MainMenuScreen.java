package com.gavinmak;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/**
 * Created by gavin on 12/24/16.
 */

public class MainMenuScreen implements Screen{
    public final Fall fall;
    public static OrthographicCamera camera;
    private static Stage stage;
    private static Table table;
    private static final int screenWidth = Gdx.graphics.getWidth(),
            screenHeight = Gdx.graphics.getHeight();

    private float alpha = 0f;
    private static ShapeRenderer fade;
    private boolean fadeStarted = false;

    private int bestScore;
    private static String startMessage = "";
    private static BitmapFont textFont, scoreFont, startFont;
    private static GlyphLayout scoreGlyph;

    private static TextButton.TextButtonStyle style;
    private static TextButton startButton;


    public MainMenuScreen(final Fall gam) {
        this.fall = gam;

        stage = new Stage();

        table = new Table();
        table.setFillParent(true);
        table.padBottom(screenHeight / 8);
        stage.addActor(table);

        Preferences prefs = Gdx.app.getPreferences("justdontfall");
        bestScore = prefs.getInteger("hi", 0);

        FreeTypeFontGenerator textGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/VT323-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter textParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        textParameter.size = (int)(screenWidth * 0.25);
        textParameter.color = Color.valueOf("#212121");
        textFont = textGenerator.generateFont(textParameter);

        textParameter.size = (int)(screenWidth * 0.3);
        textParameter.color = getColor(bestScore);
        scoreFont = textGenerator.generateFont(textParameter);

        scoreGlyph = new GlyphLayout(scoreFont, "" + bestScore);

        randWord();

        fade = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, screenWidth, screenHeight);

        startFont = new BitmapFont();
        textParameter.size = (int)(screenWidth * 0.2);
        startFont = textGenerator.generateFont(textParameter);
        textGenerator.dispose();

        style = new TextButton.TextButtonStyle();
        style.font = startFont;
        style.fontColor = Color.BLACK;
        style.downFontColor = getColor(bestScore);

        startButton = new TextButton(startMessage, style);
        startButton.padRight(screenWidth / 16);
        startButton.setY(screenHeight / 8);
        startButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                fadeStarted = true;
                return true;
            }
        });

        table.add(startButton).expand().bottom().right();

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        fall.batch.setProjectionMatrix(camera.combined);

        fall.batch.begin();
        textFont.draw(fall.batch, "just dont\n fall.", screenWidth * 0.05f, screenHeight * 0.9f);
        scoreFont.draw(fall.batch, "" + bestScore, (screenWidth - scoreGlyph.width) * 0.5f, screenHeight * 0.55f);
        fall.batch.end();

        stage.draw();

        Gdx.graphics.getGL20().glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        fade.begin(ShapeRenderer.ShapeType.Filled);
        fade.setColor(new Color(0, 0, 0, alpha - 0.2f));
        fade.rect(0, 0, screenWidth, screenHeight);
        fade.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if(fadeStarted)
            alpha += 0.02;

        if(alpha > 1.2) {
            fall.setScreen(new GameScreen(fall));
        }
    }

    private static void randWord() {
        int r = (int)Math.floor(Math.random() * 6);
        switch (r) {
            case 0: startMessage = ">okay"; break;
            case 1: startMessage = ">i wont"; break;
            case 2: startMessage = ">alright"; break;
            case 3: startMessage = ">sure"; break;
            case 4: startMessage = ">k"; break;
            case 5: startMessage = ">lets go"; break;
        }
    }

    private static Color getColor(int score) {
        if(score < 1000) {
            // grey
            return Color.valueOf("#555555");
        }

        else if(score < 2000) {
            // yellow
            return Color.valueOf("#ff3b62");
        }

        else if(score < 3000) {
            // blue
            return Color.valueOf("#FF42A7");
        }

        else if(score < 4000) {
            // purple
            return Color.valueOf("#FFE65b");
        }

        else if(score < 5000) {
            // orange
            return Color.valueOf("#02b042");
        }

        else if(score < 6000) {
            // red
            return Color.valueOf("#53FF53");
        }

        else {
            // black
            return Color.valueOf("#e0e0e0");
        }

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void show() {

    }


    @Override
    public void dispose() {

    }
}
