package au.com.reece.deliveryengineering.bamboospecs.models;

import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.UserPasswordCredentials;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

public class PermissionFileModel extends DomainModel {
    @NotNull
    @NotEmpty
    public String bambooServer;

    @NotNull
    @NotEmpty
    public Set<@Valid ProjectPermissionModel> projects;

    @NotNull
    @NotEmpty
    public Set<@Valid DeploymentPermissionModel> deployments;

    public void publish(BambooServer bambooServer, UserPasswordCredentials adminUser) {
        this.projects.forEach(x -> x.publishPermissions(bambooServer, adminUser));
        this.deployments.forEach(x -> x.publishPermissions(bambooServer, adminUser));
    }
}
