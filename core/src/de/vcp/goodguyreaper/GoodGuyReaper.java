package de.vcp.goodguyreaper;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
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

    //movement
    private boolean leftMove        = false;
    private boolean rightMove       = false;
    private boolean forwardMove     = false;
    private boolean backwardMove    = false;
    //TODO: fix rotation
    private boolean rotateRight     = false;
    private boolean rotateLeft      = false;

    private Vector3 source          = new Vector3(0f,0f,0f);
    private Vector3 yAxis           = new Vector3(0f,1f,0f);

    private boolean collision;
    private ModelInstance playerInstance;
    private ModelInstance platformInstance;
    private btCollisionShape ballShape;
    private btCollisionShape groundShape;
    private btCollisionObject platformObject;
    private btCollisionObject playerObject;

    private btDefaultCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;

    @Override
    public void create() {

        Bullet.init();

        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);

        //init collision shapes
        ballShape = new btSphereShape(0.5f);
        groundShape = new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f));


        //create playerCamera
        playerCamera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        playerCamera.position.set(0f, 10f, 10f);
        playerCamera.lookAt(source);
        playerCamera.near = 0.1f;
        playerCamera.far = 100f;

        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();

        platformModel = modelBuilder.createBox(5f, 1f, 5f, new Material(ColorAttribute.createDiffuse(Color.CYAN)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        platformInstance = new ModelInstance(platformModel, source);
        modelInstances.add(platformInstance);

        //loading .obj files like described in https://xoppa.github.io/blog/loading-models-using-libgdx/
        //TODO: .obj files are good for testing but nothing more... you need to generate g3dj files from fbx https://github.com/libgdx/fbx-conv
        assets = new AssetManager();
        assets.load("core/assets/models/reaper/reaper.obj", Model.class);
        isLoading = true;

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        Gdx.input.setInputProcessor(this);

        platformObject = new btCollisionObject();
        platformObject.setCollisionShape(groundShape);
        platformObject.setWorldTransform(platformInstance.transform);
    }

    private void doneLoading() {
        playerModel = assets.get("core/assets/models/reaper/reaper.obj", Model.class);
        playerInstance = new ModelInstance(playerModel, 0f, 8f, 0f);

        modelInstances.add(playerInstance);

        playerObject = new btCollisionObject();
        //obtain collision sphere from model nodes
        playerObject.setCollisionShape(Bullet.obtainStaticNodeShape(playerModel.nodes));
        playerObject.setWorldTransform(playerInstance.transform);

        isLoading = false;
    }

    @Override
    public void render() {
        if (isLoading && assets.update())
            doneLoading();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);

        //factorMovementPositive += 5 * Gdx.graphics.getDeltaTime();
        //factorMovementNegative -= 5 * Gdx.graphics.getDeltaTime();

        if (leftMove) {
            playerCamera.translate(-0.1f, 0f,0f);
            playerInstance.transform.translate(-0.1f, 0f,0f);
        }
        if (rightMove) {
            playerCamera.translate(0.1f, 0f, 0f);
            playerInstance.transform.translate(0.1f, 0f,0f);
        }
        if (forwardMove) {
            playerCamera.translate(0f, 0f, -0.1f);
            playerInstance.transform.translate(0f, 0f,-0.1f);
        }
        if (backwardMove) {
            playerCamera.translate(0f, 0f, 0.1f);
            playerInstance.transform.translate(0f, 0f,0.1f);
        }
        //TODO: probably rotate only player/camera?
        //disabled atm due to movement bugs
        if (rotateLeft) {
            //playerCamera.rotate(yAxis, -1f);
            //playerInstance.transform.rotate(yAxis, 1f);
        }
        if (rotateRight) {
            //playerCamera.rotate(yAxis, 1f);
            //playerInstance.transform.rotate(yAxis, -1f);
        }

        //bullet engine test from tutorial: https://xoppa.github.io/blog/using-the-libgdx-3d-physics-bullet-wrapper-part1/
        final float delta = Math.min(1f/30f, Gdx.graphics.getDeltaTime());

        if (!collision) {
            if (playerInstance != null) {
                playerInstance.transform.translate(0f, -delta, 0f);
                playerObject.setWorldTransform(playerInstance.transform);
            }
        }
        if (playerObject != null) {
            collision = checkCollision(playerObject, platformObject);
        }

        playerCamera.update();
        modelBatch.begin(playerCamera);
        modelBatch.render(modelInstances, environment);
        modelBatch.end();
    }

    private boolean checkCollision(btCollisionObject obj0, btCollisionObject obj1) {
        CollisionObjectWrapper co0 = new CollisionObjectWrapper(obj0);
        CollisionObjectWrapper co1 = new CollisionObjectWrapper(obj1);

        btCollisionAlgorithm algorithm = dispatcher.findAlgorithm(co0.wrapper, co1.wrapper, dispatcher.getNewManifold(obj0, obj1), ebtDispatcherQueryType.BT_CLOSEST_POINT_ALGORITHMS);

        btDispatcherInfo info = new btDispatcherInfo();
        btManifoldResult result = new btManifoldResult(co0.wrapper, co1.wrapper);

        algorithm.processCollision(co0.wrapper, co1.wrapper, info, result);

        boolean r = result.getPersistentManifold().getNumContacts() > 0;

        dispatcher.freeCollisionAlgorithm(algorithm.getCPointer());
        result.dispose();
        info.dispose();
        co1.dispose();
        co0.dispose();

        return r;
    }

    @Override
    public void dispose() {
        platformObject.dispose();
        groundShape.dispose();

        playerObject.dispose();
        ballShape.dispose();

        dispatcher.dispose();
        collisionConfig.dispose();

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
    private void setRotateLeft(boolean b) {
        if(rotateLeft && b) rotateLeft = false;
        rotateLeft = b;
    }
    private void setRotateRight(boolean b) {
        if(rotateRight && b) rotateRight = false;
        rotateRight = b;
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
            case Input.Keys.Q:
                setRotateLeft(true);
                Gdx.app.debug("ROTATE","started - left");
                break;
            case Input.Keys.E:
                setRotateRight(true);
                Gdx.app.debug("ROTATE","started - right");
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
            case Input.Keys.Q:
                setRotateLeft(false);
                Gdx.app.debug("ROTATE","stopped - left");
                break;
            case Input.Keys.E:
                setRotateRight(false);
                Gdx.app.debug("ROTATE","stopped - right");
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
