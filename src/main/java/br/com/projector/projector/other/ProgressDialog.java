package br.com.projector.projector.other;

/*
    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    version 2 of the license, or (at your option) any later version.
 */
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;

/**
 * Dialog which displays indeterminate progress.
 *
 * @author <a href="mailto:jclasslib@ej-technologies.com">Ingo Kegel</a>
 * @version $Revision: 1.1 $ $Date: 2003/08/18 07:46:43 $
 */
public class ProgressDialog extends JDialog {

    private static final int PROGRESS_BAR_WIDTH = 200;

    public interface Executor {

        void doInBackground();

        void done();
    }

    private Executor executor;
    private Thread task;

    private JProgressBar progressBar;
    private JLabel lblMessage;
    private JButton cancelButton;

    /**
     * Constructor.
     *
     * @param parent the parent frame.
     * @param executor the <tt>Executor</tt> to be started on
     * <tt>setVisible</tt>.
     * @param message the initial status message.
     */
    public ProgressDialog(JFrame parent, Executor executor, String message) {
        super(parent);
        init(executor, message);
    }

    /**
     * Constructor.
     *
     * @param parent the parent dialog.
     * @param executor the <tt>Executor</tt> to be started on
     * <tt>setVisible</tt>.
     * @param message the initial status message.
     */
    public ProgressDialog(JDialog parent, Executor executor, String message) {
        super(parent);
        init(executor, message);
    }

    /**
     * Set the current status message.
     *
     * @param message the message.
     */
    public void setMessage(String message) {
        lblMessage.setText(message);
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    /**
     * Set the  <tt>Executor</tt> to be started on <tt>setVisible</tt>.
     *
     * @param runnable the <tt>Executor</tt>.
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            progressBar.setIndeterminate(true);
        } else {
            progressBar.setIndeterminate(false);
        }
        super.setVisible(visible);
    }

    private void init(Executor runnable, String message) {
        setupControls();
        setupComponent();
        setupEventHandlers();
        setMessage(message);
        setExecutor(runnable);
    }

    private void setupControls() {

        progressBar = new JProgressBar();
        Dimension preferredSize = progressBar.getPreferredSize();
        preferredSize.width = PROGRESS_BAR_WIDTH;
        progressBar.setPreferredSize(preferredSize);
        lblMessage = new JLabel(" ");
        cancelButton = new JButton();
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (task != null) {
                    task.interrupt();
                }
            }
        });
    }

    private void setupComponent() {

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = GridBagConstraints.RELATIVE;
        gc.anchor = GridBagConstraints.NORTHWEST;
        contentPane.add(lblMessage, gc);
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(progressBar, gc);
        gc.fill = GridBagConstraints.NONE;
        contentPane.add(cancelButton, gc);
        setTitle("");
        setModal(true);
        pack();

    }

    private void setupEventHandlers() {

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent event) {
                task = new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            executor.doInBackground();
                        } finally {
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    setVisible(false);
                                    task = null;
                                    executor.done();
                                }
                            });
                        }
                    }
                };

                task.start();
            }
        });
    }

}
