package au.com.reece.deliveryengineering.bamboospecs.models;

import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

public class StageModel extends DomainModel {
    @NotNull
    @NotEmpty
    public String name;

    @NotNull
    @NotEmpty
    public List<StageJobModel> jobs;

    public Stage asStage(Plan plan) {
        Stage stage = new Stage(this.name);
        ArrayList<Job> l = new ArrayList<>();
        for (StageJobModel job : this.jobs) {
            l.add(job.asJob(plan));
        }
        return stage.jobs(l.toArray(new Job[l.size()]));
    }
}
