package au.com.reece.de.bamboospecs.models;

import au.com.reece.de.bamboospecs.models.enums.TriggerType;
import com.atlassian.bamboo.specs.api.builders.trigger.Trigger;
import com.atlassian.bamboo.specs.builders.trigger.AfterSuccessfulBuildPlanTrigger;
import com.atlassian.bamboo.specs.builders.trigger.BitbucketServerTrigger;
import com.atlassian.bamboo.specs.builders.trigger.ScheduledTrigger;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class TriggerModel {
    @NotNull
    public TriggerType type;

    @NotNull
    @NotEmpty
    public String description;

    public String everyNumHours;
    public String dailyAt;
    public String weeklyAt;
    public String monthlyAt;
    public String cron;
    public String branch;


    static private LocalTime parseTimeNicely(String time) {
        return LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm"));
    }

    public Trigger asTrigger() {
        switch (this.type) {
            case AFTER_SUCCESSFUL_BUILD_PLAN:
                AfterSuccessfulBuildPlanTrigger trigger = new AfterSuccessfulBuildPlanTrigger();
                trigger.description(this.description);
                if (this.branch != null) {
                    trigger.triggerByBranch(this.branch);
                }
                return trigger;
            case AFTER_STASH_COMMIT:
                return new BitbucketServerTrigger().description(this.description);
            case SCHEDULED:
                ScheduledTrigger trigger = new ScheduledTrigger().description(this.description);
                if (this.everyNumHours != null) {
                    trigger.scheduleEvery(Integer.getInteger(this.everyNumHours), TimeUnit.HOURS);
                }
                if (this.dailyAt != null) {
                    trigger.scheduleOnceDaily(parseTimeNicely(dailyAt));
                }
                if (this.weeklyAt != null) {
                    String[] parts = this.weeklyAt.split(" ");
                    String upperDay = parts[0].toUpperCase();
                    trigger.scheduleWeekly(parseTimeNicely(parts[1]), DayOfWeek.valueOf(upperDay));
                }
                if (this.monthlyAt != null) {
                    String[] parts = this.monthlyAt.split(" ");
                    trigger.scheduleMonthly(parseTimeNicely(parts[1]), Integer.getInteger(parts[0]));
                }
                if (this.cron != null) {
                    trigger.cronExpression(this.cron);
                }
                return trigger;
            default:
                throw new RuntimeException("Unexpected 'type' value from yaml " + this.type);
        }
    }
}
