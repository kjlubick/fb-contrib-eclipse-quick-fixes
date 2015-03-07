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

import org.eclipse.jdt.internal.junit.model.ITestRunListener2;
import org.eclipse.jdt.internal.junit.model.RemoteTestRunnerClient;

@SuppressWarnings("restriction")
public final class PDETestResultsCollector {
    private static PDETestListener pdeTestListener;

    private String suiteName;

    private PDETestResultsCollector(String suite) {
        suiteName = suite;
    }

    private void run(int port) throws InterruptedException {
        pdeTestListener = new PDETestListener(this, suiteName);
        new RemoteTestRunnerClient().startListening(new ITestRunListener2[] {pdeTestListener}, port);
        System.out.println("Listening on port " + port + " for test suite " + suiteName + " results ...");
        synchronized (this) {
            wait();
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("usage: PDETestResultsCollector <test suite name> <port number>");
            System.exit(0);
        }

        try {
            new PDETestResultsCollector(args[0]).run(Integer.parseInt(args[1]));
        } catch (Throwable th) {
            th.printStackTrace();
        }

        if (pdeTestListener != null && pdeTestListener.failed()) {
            System.exit(1);
        }
    }
}
