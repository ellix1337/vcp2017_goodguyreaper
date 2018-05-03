package de.vcp.goodguyreaper.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
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

		new LwjglApplication(new GoodGuyReaper(), config);
	}
}
