package edu.univ.erp.ui.components;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import edu.univ.erp.net.ClientSession;

/**
 * Minimal cancellable progress dialog. Call showDialog() from EDT before starting a long operation
 * and call close() when done. Clicking Cancel will clear the client session (close persistent socket)
 * so ongoing operations that use the session will observe a closed connection.
 */
public class ProgressDialog {
    private final JDialog dlg;

    public ProgressDialog(Frame owner, String title, String message) {
        dlg = new JDialog(owner, title, Dialog.ModalityType.MODELESS);
        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dlg.setSize(300, 120);
        dlg.setLocationRelativeTo(owner);

        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.add(new JLabel(message), BorderLayout.NORTH);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        p.add(bar, BorderLayout.CENTER);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try { ClientSession.clear(); } catch (Exception ignore) {}
                dlg.dispose();
            }
        });
        p.add(cancel, BorderLayout.SOUTH);

        dlg.getContentPane().add(p);
    }

    public void showDialog() {
        if (!dlg.isVisible()) dlg.setVisible(true);
    }

    public void close() {
        if (dlg.isVisible()) dlg.dispose();
    }
}
