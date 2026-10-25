package fmi.uni.masters.behaviours;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.concurrent.CompletableFuture;

public class AuthUserBehaviour extends Behaviour {

    private AID recommenderAgent;
    private String action;
    private String username;
    private String password;
    private CompletableFuture<Boolean> futureResult;
    private int step = 0;
    private MessageTemplate mt;

    public AuthUserBehaviour(AID recommenderAgent, String action, String username, String password, CompletableFuture<Boolean> futureResult) {
        this.recommenderAgent = recommenderAgent;
        this.action = action;
        this.username = username;
        this.password = password;
        this.futureResult = futureResult;
    }

    @Override
    public void action() {
        if (step == 0) {
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(recommenderAgent);
            req.setContent(action + ":" + username + ":" + password);
            req.setConversationId("auth-conv");
            req.setReplyWith("auth" + System.currentTimeMillis());

            myAgent.send(req);

            mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId("auth-conv"),
                    MessageTemplate.MatchInReplyTo(req.getReplyWith())
            );
            step = 1;
        } else if (step == 1) {
            ACLMessage reply = myAgent.receive(mt);
            if (reply != null) {
                futureResult.complete(reply.getPerformative() == ACLMessage.INFORM);
                step = 2;
            } else {
                block();
            }
        }
    }

    @Override
    public boolean done() {
        return step == 2;
    }
}