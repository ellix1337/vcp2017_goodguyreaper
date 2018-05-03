package de.vcp.goodguyreaper.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import de.vcp.goodguyreaper.BulletCollision;
import de.vcp.goodguyreaper.BulletTutorial7;
import de.vcp.goodguyreaper.BulletTutorialFinal;
import de.vcp.goodguyreaper.GoodGuyReaper;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "GoodGuyReaper";
		config.resizable = true;
		config.width = 1024;
		config.height = 768;
		config.fullscreen = false;
		config.vSyncEnabled = true;

		//uncomment several Applications for testing

		new LwjglApplication(new GoodGuyReaper(), config);
		//new LwjglApplication(new BulletCollision(), config);
		//new LwjglApplication(new BulletTutorialFinal(), config);
		//new LwjglApplication(new BulletTutorial7(), config);
	}
}
