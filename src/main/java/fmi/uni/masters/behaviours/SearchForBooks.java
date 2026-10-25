package fmi.uni.masters.behaviours;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.concurrent.CompletableFuture;

public class SearchForBooks extends Behaviour {

    private AID recommenderAgent;
    private CompletableFuture<String> futureResult;
    private int step = 0;
    private MessageTemplate mt;

    public SearchForBooks(AID recommenderAgent, CompletableFuture<String> futureResult) {
        this.recommenderAgent = recommenderAgent;
        this.futureResult = futureResult;
    }

    @Override
    public void action() {
        switch (step) {
            case 0:
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                cfp.addReceiver(recommenderAgent);
                cfp.setContent("GET_ALL_BOOKS");
                cfp.setConversationId("book-search-conv");
                cfp.setReplyWith("req" + System.currentTimeMillis());

                myAgent.send(cfp);
                mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId("book-search-conv"),
                        MessageTemplate.MatchInReplyTo(cfp.getReplyWith())
                );
                step = 1;
                break;

            case 1:
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        futureResult.complete(reply.getContent());
                    } else {
                        futureResult.complete("[]");
                    }
                    step = 2;
                } else {
                    block();
                }
                break;
        }
    }

    @Override
    public boolean done() {
        return step == 2;
    }
}