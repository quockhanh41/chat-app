//package src.server;
//
//public class onlineUsersThread implements Runnable {
//    Server server;
//
//    public onlineUsersThread(Server server) {
//        this.server = server;
//    }
//
//    @Override
//    public void run() {
//        while (server.isRunning) {
//            broadcastOnlineUsers();
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//}
//
