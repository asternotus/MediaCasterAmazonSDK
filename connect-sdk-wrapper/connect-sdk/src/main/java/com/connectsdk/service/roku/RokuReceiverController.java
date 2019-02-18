package com.connectsdk.service.roku;

import com.connectsdk.service.SamsungSmartViewService;
import com.connectsdk.service.roku.model.CommandRequestMessage;
import com.connectsdk.service.roku.model.EventMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mega.cast.utils.log.SmartLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Bojan Kudos on 11/24/2017.
 */

public class RokuReceiverController {

    private static final String LOG_TAG = RokuReceiverController.class.getSimpleName();

    private static final int COMMAND_PORT = 54321;
    private static final int EVENT_PORT = 54322;

    private static final int COMMAND_TASK_ID = 0x01;
    private static final int EVENT_TASK_ID = 0x10;

    private Gson gson;
    private String receiverAddr;
    private volatile int connectionIdStatus = 0x00;

    /**
     * Indicates the connection state towards the roku receiver app.
     * This variable is shared among the command and event thread,
     * so detecting and io error in one of them will disconnect the other one also.
     */
    private volatile boolean connected;
    private BlockingQueue<CommandRequestMessage> queuedCommandRequests = new LinkedBlockingQueue<>();
    private CopyOnWriteArrayList<MessageListener> messageListeners
            = new CopyOnWriteArrayList<>();

    private CommandSenderThread commandSenderThread;
    private EventReceiverThread eventReceiverThread;

    private ConnectionStateListener connectionStateListener;

    public RokuReceiverController() {
        gson = new GsonBuilder().create();
    }

    public void connectToReceiver() {
        if (connected) {
            SmartLog.d(LOG_TAG, "The receiver is already connected?");
            // TODO: 11/24/2017 See is this ever happen and if so, what to do in such case
        }

        connected = true;
        queuedCommandRequests.clear();

        commandSenderThread = new CommandSenderThread();
        eventReceiverThread = new EventReceiverThread();

        commandSenderThread.start();
        eventReceiverThread.start();
    }

    public void disconnectFromReceiver() {
        if (!connected) {
            SmartLog.d(LOG_TAG, "The receiver is already disconnected");
            return;
        }
        executeDisconnectProcedure();
    }

    private void executeDisconnectProcedure() {
        connected = false;
        connectionIdStatus = 0x00;
        commandSenderThread.interrupt();
        eventReceiverThread.interruptSocketConnection();

        commandSenderThread = null;
        eventReceiverThread = null;

        queuedCommandRequests.clear();
    }

    /**
     * Report that the thread has successfully opened a socket connection to the receiver app
     * @param taskId
     */
    private synchronized void reportConnected(int taskId) {
        connectionIdStatus |= taskId;
        if (connectionIdStatus == (COMMAND_TASK_ID | EVENT_TASK_ID)) { // Both threads/tasks are connected
            SmartLog.d(this, "Notifying of task connection success " + connectionStateListener);
            if (connectionStateListener != null) {
                connectionStateListener.onSuccessfulConnection();
            }
        }
    }

    /**
     * Reports that the thread has detected a forced disconnection from the receiver app.
     * <p>The {@link #connectionStateListener} will be notified upon first disconnection and will terminate the {@link #connected} status
     * thus breaking the thread/task execution loops</p>
     * @param taskId
     */
    private synchronized void reportDisconnected(int taskId) {
        SmartLog.d(this, "Receiver Communication Thread {1} got disconnected" , taskId);
        connectionIdStatus &= ~taskId;
        if (connected && connectionStateListener != null) {
            executeDisconnectProcedure();
            connectionStateListener.onForcedDisconnection();
        }
    }

    public void sendCommand(CommandRequestMessage requestMessage) {
        if (!connected) {
            SmartLog.w(LOG_TAG, "Cant send commands, receiver is disconnected.");
            return;
        }
        queuedCommandRequests.offer(requestMessage);
    }

    public ConnectionStateListener getConnectionStateListener() {
        return connectionStateListener;
    }

    public void setConnectionStateListener(ConnectionStateListener connectionStateListener) {
        this.connectionStateListener = connectionStateListener;
    }

    public String getReceiverAddr() {
        return receiverAddr;
    }

    public void setReceiverAddr(String receiverAddr) {
        this.receiverAddr = receiverAddr;
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    //----------------------------------------------------------------------------------------------
    // Communication Tasks/Threads
    //----------------------------------------------------------------------------------------------

    private class CommandSenderThread extends AbstractReceiverCommunicationThread {

        public CommandSenderThread() {
            super(COMMAND_PORT, COMMAND_TASK_ID);
        }

        @Override
        protected void executeTask() {
            PrintWriter out = null;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                SmartLog.e(this, e);
                reportDisconnected(taskId);
                return;
            }

            while (connected) {
                try {
                    CommandRequestMessage message = queuedCommandRequests.take();
                    String jsonMsg = gson.toJson(message);
                    SmartLog.d(LOG_TAG, "Sending command request msg: " + jsonMsg);
                    out.println(jsonMsg);

                    // Writing to Roku BrightScript without delay sometimes causes the buffer to read multiple commands at once
                    // causing a json parsing issue on the receiver side.
                    // TODO: 11/29/2017 If the sleep interval doesnt prevent the problem, implement a byte by byte reading on the receiver side and manually parsing the end line delimiter and queuing of commands
                    sleep(50);
                } catch (InterruptedException e) {
                    SmartLog.e(this, e);
                    break;
                }
            }
        }
    }

    private class EventReceiverThread extends AbstractReceiverCommunicationThread {

        // We dont want to report the disconnection event if it was invoked manually
        private boolean externalInterrupt = false;

        public EventReceiverThread() {
            super(EVENT_PORT, EVENT_TASK_ID);
        }

        @Override
        protected void executeTask() {
            BufferedReader socketReader = null;
            try {
                socketReader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                SmartLog.e(this, e);
                reportDisconnected(taskId);
                return;
            }

            while (connected) {
                try {
                    String msg = socketReader.readLine();
                    if (msg == null && !externalInterrupt) {
                        reportDisconnected(taskId);
                        break;
                    }
                    EventMessage eventMessage = gson.fromJson(msg, EventMessage.class);
                    if (eventMessage == null || eventMessage.getMessageType() == null) {
                        // Can happen if the user messes with receiver app manually and we receive an empty string msg
                        // before we receive the disconnect event
                        continue;
                    }
                    SmartLog.d(this, "Received msg from receiver app: " + eventMessage.getMessageType());
                    SmartLog.d(this, "Received msg from receiver app: " + eventMessage.getData().toString());
                    for (MessageListener messageListener : messageListeners) {
                        if (messageListener != null) {
                            messageListener.onMessageReceived(eventMessage);
                        }
                    }
                } catch (IOException e) {
                    SmartLog.e(this, e);
                    if (!externalInterrupt) {
                        reportDisconnected(taskId);
                    }
                }
            }
        }

        /**
         * A BufferedReader readLine method cant be interrupted via {@link #interrupt()} and the only
         * way to interrupt is by closing the socket and invoking the IOException.
         */
        private void interruptSocketConnection() {
            externalInterrupt = true;
            closeSocketConnection();
        }
    }

    /**
     * Abstracts the socket connection/disconnection process for "Receiver Communication Threads"
     * <p>Upon starting a ReceiverCommunication thread a socket connection established towards the receiverAddr on the specified port.
     * Then {@link #executeTask()} is invoked followed by {@link #closeSocketConnection()}</p>
     */
    private abstract class AbstractReceiverCommunicationThread extends Thread {

        protected Socket socket = null;
        protected int port;
        protected int taskId;

        public AbstractReceiverCommunicationThread(int port, int taskId) {
            this.port = port;
            this.taskId = taskId;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(receiverAddr, port);
            } catch (IOException e) {
                SmartLog.e(this, "Error initiating socket connection with the receiver on [PORT] " + port, e);
                reportDisconnected(COMMAND_TASK_ID);
                return;
            }

            SmartLog.d(this, "Socket connection established with Roku Recevier on [PORT] " + port);
            reportConnected(taskId);

            executeTask();

            closeSocketConnection();
        }

        protected boolean isConnected() {
            return socket != null && socket.isConnected() && !socket.isClosed();
        }

        /**
         * Implement the actual work that needs to be execute after the socket connection has been established.
         * <p>The socket connection will be established before this method is called and will be closed after its finished</p>
         */
        protected abstract void executeTask();

        protected void closeSocketConnection() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    SmartLog.e(this, e);
                }
                socket = null;
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Public listener interfaces
    //----------------------------------------------------------------------------------------------

    public interface ConnectionStateListener {
        void onSuccessfulConnection();
        void onForcedDisconnection();
    }

    public interface MessageListener {
        void onMessageReceived(EventMessage eventMessage);
    }
}
