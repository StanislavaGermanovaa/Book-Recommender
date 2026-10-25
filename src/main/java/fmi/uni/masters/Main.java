package fmi.uni.masters;

import fmi.uni.masters.agent.ClientAgent;
import fmi.uni.masters.agent.BookRecommenderAgent;
import jade.core.ProfileImpl;
import jade.core.Profile;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class Main {
    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "9898");
        profile.setParameter(Profile.GUI, "true");

        AgentContainer mainContainer = runtime.createMainContainer(profile);

        try {
            AgentController agency = mainContainer.createNewAgent(
                    "BookAgency", BookRecommenderAgent.class.getName(), null);
            AgentController client = mainContainer.createNewAgent(
                    "BookCustomer", ClientAgent.class.getName(), null);
            agency.start();
            client.start();

        } catch (Exception e) {
            System.err.println("Error occurred while starting JADE container!");
            e.printStackTrace();
        }
    }
}