package com.gavinmak;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

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
    private static BitmapFont scoreFont;
    private static Image logo;
    private static ImageButton startButton;
    private static Label score;

    public MainMenuScreen(final Fall gam) {
        this.fall = gam;

        stage = new Stage();

        table = new Table();
        table.setFillParent(true);
        table.padBottom(screenHeight / 8);
        stage.addActor(table);

        logo = new Image(new Texture(Gdx.files.internal("justdontfall.png")));
        logo.setScaling(Scaling.fit);
        table.add(logo).expand().top().left().fill().pad(screenWidth / 16).row();

        Preferences prefs = Gdx.app.getPreferences("justdontfall");
        bestScore = prefs.getInteger("hi", 0);
        FreeTypeFontGenerator textGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/VT323-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter textParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        textParameter.size = (int)(screenWidth * 0.3);
        textParameter.color = getColor(bestScore);
        scoreFont = textGenerator.generateFont(textParameter);
        textGenerator.dispose();

        score = new Label("" + bestScore, new Label.LabelStyle(scoreFont, getColor(bestScore)));
        table.add(score).expand().row();

        fade = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, screenWidth, screenHeight);

        startButton = new ImageButton(new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("start.png")))));
        startButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                fadeStarted = true;
                return true;
            }
        });

        table.add(startButton).expand().bottom().right().fill().padLeft(screenWidth / 3).padTop(screenHeight / 16);

        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        fall.batch.setProjectionMatrix(camera.combined);

        stage.draw();

        // black screen fade
        Gdx.graphics.getGL20().glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        fade.begin(ShapeRenderer.ShapeType.Filled);
        fade.setColor(new Color(0, 0, 0, alpha - 0.2f));
        fade.rect(0, 0, screenWidth, screenHeight);
        fade.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // begins the fade, increases opacity of black screen
        if(fadeStarted)
            alpha += 0.02;

        // once the fade is done, start a game screen
        if(alpha > 1.2) {
            fall.setScreen(new GameScreen(fall));
            dispose();
        }
    }

    // sets the color for the score text and start text
    private static Color getColor(int score) {
        if(score < 1000) {
            return Color.valueOf("#555555");
        }

        else if(score < 2000) {
            return Color.valueOf("#FF00900");
        }

        else if(score < 3000) {
            return Color.valueOf("#FF9200");
        }

        else if(score < 4000) {
            return Color.valueOf("#FFD300");
        }

        else if(score < 5000) {
            return Color.valueOf("#CCF600");
        }

        else if(score < 6000) {
            return Color.valueOf("#00CC00");
        }

        else {
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
        stage.dispose();
        scoreFont.dispose();
    }
}
