/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Originally from the Eclipse.org article Automating Eclipse PDE Unit Tests using Ant by Brian Joyce
 *  https://www.eclipse.org/articles/article.php?file=Article-PDEJUnitAntAutomation/index.html
 *  https://web.archive.org/web/20130924110132/http://www.eclipse.org/articles/article.php?file=Article-PDEJUnitAntAutomation/index.html
 *******************************************************************************/
package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;

public class PDETestPortLocator {

    public static void main(String[] args) {
        new PDETestPortLocator().savePortToFile();
    }

    public void savePortToFile() {
        int port = locatePDETestPortNumber();
        File propsFile = new File("pde_test_port.properties");
        System.out.println("PDE Test port: " + port);
        OutputStream os = null;
        try {
            os = new FileOutputStream(propsFile);
            os.write(new String("pde.test.port=" + port).getBytes());
            os.flush();
            System.out.println("PDE Test port saved to file " + propsFile.getAbsolutePath());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
            os = null;
        }
    }

    private int locatePDETestPortNumber() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            // ignore
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return -1;
    }
}
