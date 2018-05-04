package de.vcp.goodguyreaper;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;

public class GoodGuyReaper extends ApplicationAdapter implements InputProcessor {

    private PerspectiveCamera playerCamera;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private Array<ModelInstance> modelInstances = new Array<ModelInstance>();
    private Model platformModel;
    private Model playerModel;
    private Model heartModel;
    private Environment environment;
    private AssetManager assets;
    private boolean isLoading;

    ArrayMap<String, GameObject.Constructor> constructors;

    //movement
    private boolean leftMove = false;
    private boolean rightMove = false;
    private boolean forwardMove = false;
    private boolean backwardMove = false;
    //TODO: fix rotation - maybe need to use trn() method instead of translate()
    private boolean rotateRight = false;
    private boolean rotateLeft = false;

    private Vector3 source = new Vector3(0f, 0f, 0f);
    private Vector3 yAxis = new Vector3(0f, 1f, 0f);

    private boolean collision;
    private ModelInstance playerInstance;
    private ModelInstance platformInstance;
    private ModelInstance heartInstance;
    private btCollisionShape ballShape;
    private btCollisionShape groundShape;
    private btCollisionObject platformObject;
    private btCollisionObject playerObject;
    private btCollisionObject heartObject;

    private btDefaultCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;

    private MyContactListener contactListener;

    @Override
    public void create() {

        Bullet.init();
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        contactListener = new MyContactListener();

        //init collision shapes
        ballShape = new btSphereShape(0.5f);
        groundShape = new btBoxShape(new Vector3(5f, 0.5f, 5f));

        playerCameraInit();
        modelsInit();
        constructorsInit();

        Gdx.input.setInputProcessor(this);

        modelBatch = new ModelBatch();

        //loading .obj files like described in https://xoppa.github.io/blog/loading-models-using-libgdx/
        //TODO: .obj files are good for "testing" but nothing more... you need to generate g3dj files from fbx https://github.com/libgdx/fbx-conv
        assets = new AssetManager();
        assets.load("core/assets/models/reaper/reaper.obj", Model.class);
        assets.load("core/assets/models/heart/Heart.obj", Model.class);
        isLoading = true;

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        platformObject = new btCollisionObject();
        platformObject.setCollisionShape(groundShape);
        platformObject.setWorldTransform(platformInstance.transform);
    }

    /**
     * inits the models
     */
    private void modelsInit() {

        modelBuilder = new ModelBuilder();
        platformModel = modelBuilder.createBox(10f, 1f, 10f, new Material(ColorAttribute.createDiffuse(Color.CYAN)), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        platformInstance = new ModelInstance(platformModel, source);
        modelInstances.add(platformInstance);

        //TODO: remove - test purpose only
        //modelInstances.add(new ModelInstance(platformModel, new Vector3(0f, 3f, 0f)));

        for (int x=-12; x <= 12; x += 12) {
            for (int z=-12; z <= 12; z += 12) {
                ModelInstance instance = new ModelInstance(platformModel, x, 0, z);
                if (x == 0) {
                    if (z != 0) {
                        instance.model.materials.add(new Material(ColorAttribute.createDiffuse(Color.GOLDENROD)));
                    }
                } else {
                    if (z!= 0) {
                        instance.model.materials.add(new Material(ColorAttribute.createAmbient(Color.PURPLE)));
                    }
                }
                modelInstances.add(instance);
            }
        }
    }

    /**
     * inits constructors for every GameObject
     */
    private void constructorsInit() {
        //fixme: doesnt work properly because tutorial implementation is deprecated and i dont want to use it therefore
        constructors = new ArrayMap<String, GameObject.Constructor>(String.class, GameObject.Constructor.class);
        constructors.put("platform", new GameObject.Constructor(platformModel, "platform", new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f))));

        GameObject obj = constructors.values[0].construct();
        obj.transform.setToTranslation(0f,3f,0f);
        modelInstances.add(obj);
    }

    /**
     * inits default playercamera settings
     */
    private void playerCameraInit() {
        playerCamera = new PerspectiveCamera(75, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        playerCamera.position.set(0f, 10f, 10f);
        playerCamera.lookAt(source);
        playerCamera.near = 0.1f;
        playerCamera.far = 100f;
    }

    /**
     * assigns model values after loading process
     */
    private void doneLoading() {
        playerModel = assets.get("core/assets/models/reaper/reaper.obj", Model.class);
        playerInstance = new ModelInstance(playerModel, 0f, 4f, 0f);
        //fixme: dont scale playermodel it will break collision sphere
//        playerInstance.transform.scale(2f, 2f, 2f);
        playerObject = new btCollisionObject();
        //obtain collision sphere from model nodes
        playerObject.setCollisionShape(Bullet.obtainStaticNodeShape(playerModel.nodes));
        playerObject.setWorldTransform(playerInstance.transform);

        heartModel = assets.get("core/assets/models/heart/Heart.obj", Model.class);
        heartInstance = new ModelInstance(heartModel, 2f, 1f, 2);
        heartInstance.transform.scale(0.003f, 0.003f, 0.003f);
        heartObject = new btCollisionObject();
        heartObject.setCollisionShape(ballShape);
        heartObject.setWorldTransform(heartInstance.transform);

        modelInstances.add(heartInstance);
        modelInstances.add(playerInstance);

        isLoading = false;
    }

    @Override
    public void render() {
        if (isLoading && assets.update())
            doneLoading();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        if (playerInstance != null) {
            checkMovement();
        }

        //bullet engine test from tutorial: https://xoppa.github.io/blog/using-the-libgdx-3d-physics-bullet-wrapper-part1/
        final float delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());

        if (!collision) {
            if (playerInstance != null) {
                playerInstance.transform.translate(0f, -delta*2, 0f);
                playerObject.setWorldTransform(playerInstance.transform);
            }
        }
        if (playerObject != null) {
            collision = checkCollision(playerObject, platformObject);
            //fixme: get to work
            //this should remove the heartInstance and perform PickUp action
            boolean pickUp = checkCollision(playerObject, heartObject);
            if (pickUp) {
                if (heartInstance!=null) {
                    heartInstance = null;
                    //performHeartPickUp();
                }
            }
        }

        playerCamera.update();
        modelBatch.begin(playerCamera);
        modelBatch.render(modelInstances, environment);
        modelBatch.end();
    }

    /**
     * checks if user input is triggering movement
     */
    private void checkMovement() {
        if (leftMove) {
            playerCamera.translate(-0.1f, 0f, 0f);
            playerInstance.transform.translate(-0.1f, 0f, 0f);
        }
        if (rightMove) {
            playerCamera.translate(0.1f, 0f, 0f);
            playerInstance.transform.translate(0.1f, 0f, 0f);
        }
        if (forwardMove) {
            playerCamera.translate(0f, 0f, -0.1f);
            playerInstance.transform.translate(0f, 0f, -0.1f);
        }
        if (backwardMove) {
            playerCamera.translate(0f, 0f, 0.1f);
            playerInstance.transform.translate(0f, 0f, 0.1f);
        }
        //disabled atm due to movement bugs
        //TODO: probably rotate only player/camera?
        if (rotateLeft) {
            //playerCamera.rotate(yAxis, -1f);
            //playerInstance.transform.rotate(yAxis, 1f);
        }
        if (rotateRight) {
            //playerCamera.rotate(yAxis, 1f);
            //playerInstance.transform.rotate(yAxis, -1f);
        }
    }

    /**
     * Collision detection between two objects, as shown in
     * https://xoppa.github.io/blog/using-the-libgdx-3d-physics-bullet-wrapper-part1/ tutorial but with small adjustments
     *
     * @param obj0 first object to check collision
     * @param obj1 second object to check collision
     * @return if they are colliding
     */
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
        contactListener.dispose();
    }

    public void setLeftMove(boolean b) {
        if (rightMove && b) rightMove = false;
        leftMove = b;
    }

    public void setRightMove(boolean b) {
        if (leftMove && b) leftMove = false;
        rightMove = b;
    }

    private void setForwardMove(boolean b) {
        if (forwardMove && b) forwardMove = false;
        forwardMove = b;
    }

    private void setBackwardMove(boolean b) {
        if (backwardMove && b) backwardMove = false;
        backwardMove = b;
    }

    private void setRotateLeft(boolean b) {
        if (rotateLeft && b) rotateLeft = false;
        rotateLeft = b;
    }

    private void setRotateRight(boolean b) {
        if (rotateRight && b) rotateRight = false;
        rotateRight = b;
    }

    @Override
    public boolean keyDown(int keycode) {

        switch (keycode) {
            case Input.Keys.LEFT:
            case Input.Keys.A:
                setLeftMove(true);
                Gdx.app.debug("MOVEMENT", "started - left");
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                setRightMove(true);
                Gdx.app.debug("MOVEMENT", "started - right");
                break;
            case Input.Keys.UP:
            case Input.Keys.W:
                setForwardMove(true);
                Gdx.app.debug("MOVEMENT", "started - forward");
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                setBackwardMove(true);
                Gdx.app.debug("MOVEMENT", "started - backward");
                break;
            case Input.Keys.Q:
                setRotateLeft(true);
                Gdx.app.debug("ROTATE", "started - left");
                break;
            case Input.Keys.E:
                setRotateRight(true);
                Gdx.app.debug("ROTATE", "started - right");
                break;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.LEFT:
            case Input.Keys.A:
                setLeftMove(false);
                Gdx.app.debug("MOVEMENT", "stopped - left");
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                setRightMove(false);
                Gdx.app.debug("MOVEMENT", "stopped - right");
                break;
            case Input.Keys.UP:
            case Input.Keys.W:
                setForwardMove(false);
                Gdx.app.debug("MOVEMENT", "stopped - forward");
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                setBackwardMove(false);
                Gdx.app.debug("MOVEMENT", "stopped - backward");
                break;
            case Input.Keys.Q:
                setRotateLeft(false);
                Gdx.app.debug("ROTATE", "stopped - left");
                break;
            case Input.Keys.E:
                setRotateRight(false);
                Gdx.app.debug("ROTATE", "stopped - right");
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

    class MyContactListener extends ContactListener {
        @Override
        public boolean onContactAdded(btManifoldPoint cp, btCollisionObjectWrapper colObj0Wrap, int partId0, int index0, btCollisionObjectWrapper colObj1Wrap, int partId1, int index1) {
//            modelInstances.get(colObj0Wrap.getCollisionObject().getUserValue()).moving = false;
//            modelInstances.get(colObj1Wrap.getCollisionObject().getUserValue()).moving = false;
            return true;
        }
    }
}
