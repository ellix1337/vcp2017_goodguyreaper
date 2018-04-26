package de.vcp.goodguyreaper;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;

public class GoodGuyReaper extends ApplicationAdapter implements InputProcessor {

    private PerspectiveCamera playerCamera;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private Array<ModelInstance> modelInstances = new Array<ModelInstance>();
    private Model platformModel;
    private Model playerModel;
    private Environment environment;
    private AssetManager assets;
    private boolean isLoading;
    //libgdx default members
    private SpriteBatch spriteBatch;
    private Texture img;
    //movement fields
    private float factorMovement    = 5;

    private boolean leftMove        = false;
    private boolean rightMove       = false;
    private boolean forwardMove     = false;
    private boolean backwardMove    = false;

    @Override
    public void create() {
        //libgdx default
        /*
        spriteBatch = new SpriteBatch();
        img = new Texture("core/assets/badlogic.jpg");
        */

        //create playerCamera
        playerCamera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        playerCamera.position.set(0f, 10f, 10f);
        playerCamera.lookAt(0f, 0f, 0f);
        playerCamera.near = 0.1f;
        playerCamera.far = 100f;

        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();

        platformModel = modelBuilder.createBox(5f, 5f, 5f, new Material(ColorAttribute.createDiffuse(Color.CYAN)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        assets = new AssetManager();
        assets.load("core/assets/models/reaper/reaper_model_triangulated.obj", Model.class);
        isLoading = true;

        modelInstances.add(new ModelInstance(platformModel, 0f, 0f, 0f));

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        Gdx.input.setInputProcessor(this);
    }

    private void doneLoading() {
        Model playerModel = assets.get("core/assets/models/reaper/reaper_model_triangulated.obj", Model.class);
        ModelInstance playerInstance = new ModelInstance(playerModel, 0f, 5f, 0f);
        modelInstances.add(playerInstance);
        isLoading = false;
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);

        /*
        spriteBatch.begin();
        spriteBatch.draw(img, 50, 50);
        spriteBatch.end();
        */

        //factorMovementPositive += 5 * Gdx.graphics.getDeltaTime();
        //factorMovementNegative -= 5 * Gdx.graphics.getDeltaTime();

        if (leftMove) {
            playerCamera.translate(-0.1f, 0f,0f);
        }
        if (rightMove) {
            playerCamera.translate(0.1f, 0f, 0f);
        }
        if (forwardMove) {
            playerCamera.translate(0f, 0f, -0.1f);
        }
        if (backwardMove) {
            playerCamera.translate(0f, 0f, 0.1f);
        }

        playerCamera.update();
        modelBatch.begin(playerCamera);
        modelBatch.render(modelInstances, environment);
        modelBatch.end();
    }

    @Override
    public void dispose() {
        /*
        spriteBatch.dispose();
        img.dispose();
        */
        platformModel.dispose();
        modelBatch.dispose();
        assets.dispose();
    }

    public void setLeftMove(boolean b)
    {
        if(rightMove && b) rightMove = false;
        leftMove = b;
    }
    public void setRightMove(boolean b)
    {
        if(leftMove && b) leftMove = false;
        rightMove = b;
    }
    private void setForwardMove(boolean b) {
        if(forwardMove && b) forwardMove = false;
        forwardMove = b;
    }
    private void setBackwardMove(boolean b) {
        if(backwardMove && b) backwardMove = false;
        backwardMove = b;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT: case Input.Keys.A:
                setLeftMove(true);
                Gdx.app.debug("MOVEMENT","started - left");
                break;
            case Input.Keys.RIGHT: case Input.Keys.D:
                setRightMove(true);
                Gdx.app.debug("MOVEMENT","started - right");
                break;
            case Input.Keys.UP: case Input.Keys.W:
                setForwardMove(true);
                Gdx.app.debug("MOVEMENT","started - forward");
                break;
            case Input.Keys.DOWN: case Input.Keys.S:
                setBackwardMove(true);
                Gdx.app.debug("MOVEMENT","started - backward");
                break;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT: case Input.Keys.A:
                setLeftMove(false);
                Gdx.app.debug("MOVEMENT","stopped - left");
                break;
            case Input.Keys.RIGHT: case Input.Keys.D:
                setRightMove(false);
                Gdx.app.debug("MOVEMENT","stopped - right");
                break;
            case Input.Keys.UP: case Input.Keys.W:
                setForwardMove(false);
                Gdx.app.debug("MOVEMENT","stopped - forward");
                break;
            case Input.Keys.DOWN: case Input.Keys.S:
                setBackwardMove(false);
                Gdx.app.debug("MOVEMENT","stopped - backward");
                break;
        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}