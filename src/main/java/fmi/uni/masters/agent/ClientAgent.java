package fmi.uni.masters.agent;

import fmi.uni.masters.behaviours.AuthUserBehaviour;
import fmi.uni.masters.behaviours.InteractWithBookBehaviour;
import fmi.uni.masters.behaviours.SearchForBooks;
import fmi.uni.masters.gui.ClientWebServer;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.concurrent.CompletableFuture;

public class ClientAgent extends Agent {

    private AID recommenderAgency;

    @Override
    protected void setup() {
        doWait(4000);

        DFAgentDescription da = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-recommender");
        da.addServices(sd);

        try {
            DFAgentDescription[] descrs = DFService.search(this, da);
            if (descrs.length > 0) {
                recommenderAgency = descrs[0].getName();
                System.out.println("ClientAgent - found -> " + recommenderAgency.getLocalName());

                ClientWebServer.start(this);

            } else {
                System.out.println("ClientAgent - error ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<String> getBooksFromRecommender() {
        CompletableFuture<String> futureResult = new CompletableFuture<>();
        if (recommenderAgency != null) {
            addBehaviour(new SearchForBooks(recommenderAgency, futureResult));
        } else {
            futureResult.complete("[]");
        }
        return futureResult;
    }
    public CompletableFuture<Boolean> performAuth(String action, String username, String password) {
        CompletableFuture<Boolean> futureResult = new CompletableFuture<>();
        if (recommenderAgency != null) {
            addBehaviour(new AuthUserBehaviour(recommenderAgency, action, username, password, futureResult));
        } else {
            futureResult.complete(false);
        }
        return futureResult;
    }

    public CompletableFuture<Boolean> modifyBookLikeStatus(String action, String username, String bookTitle) {
        CompletableFuture<Boolean> futureResult = new CompletableFuture<>();
        if (recommenderAgency != null) {
            addBehaviour(new InteractWithBookBehaviour(recommenderAgency, action, username, bookTitle, futureResult));
        } else {
            futureResult.complete(false);
        }
        return futureResult;
    }
}