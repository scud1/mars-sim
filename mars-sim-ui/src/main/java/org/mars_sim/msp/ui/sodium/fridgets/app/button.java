package org.mars_sim.msp.ui.sodium.fridgets.app;


import javax.swing.*;

import org.mars_sim.msp.core.sodium.*;
import org.mars_sim.msp.ui.sodium.fridgets.*;

public class button {
    public static void main(String[] args) {
        JFrame frame = new JFrame("button");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(Transaction.run(() -> {
            FrButton b = new FrButton(new Cell<>("OK"));
            Listener l = b.sClicked.listen(
                u -> System.out.println("clicked!"));
            return new FrView(frame, b) {
                public void removeNotify() {
                    super.removeNotify();
                    l.unlisten();
                }
            };
        }));
        frame.setSize(360,120);
        frame.setVisible(true);
    }
}

