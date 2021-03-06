/*
 * Copyright 2012 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.task.commands;

import java.util.List;
import javax.enterprise.util.AnnotationLiteral;
import org.drools.command.Context;
import org.jboss.seam.transaction.Transactional;
import org.jbpm.task.Group;
import org.jbpm.task.OrganizationalEntity;
import org.jbpm.task.Status;
import org.jbpm.task.Task;
import org.jbpm.task.User;
import org.jbpm.task.events.AfterTaskStartedEvent;
import org.jbpm.task.events.BeforeTaskStartedEvent;
import org.jbpm.task.exception.PermissionDeniedException;

/**
 * Operation.Start : [ new OperationCommand().{ status = [ Status.Ready ],
 * allowed = [ Allowed.PotentialOwner ], setNewOwnerToUser = true, newStatus =
 * Status.InProgress }, new OperationCommand().{ status = [ Status.Reserved ],
 * allowed = [ Allowed.Owner ], newStatus = Status.InProgress } ], *
 */

@Transactional
public class StartTaskCommand extends TaskCommand {


    public StartTaskCommand(long taskId, String userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    public Void execute(Context cntxt) {
        TaskContext context = (TaskContext) cntxt;
        Task task = context.getTaskQueryService().getTaskInstanceById(taskId);
        User user = context.getTaskIdentityService().getUserById(userId);
        context.getTaskEvents().select(new AnnotationLiteral<BeforeTaskStartedEvent>() {}).fire(task);
        // CHeck for potential Owner allowed (decorator?)
        boolean operationAllowed = CommandsUtil.isAllowed(user, getGroupsIds(), task.getPeopleAssignments().getPotentialOwners());
        if (!operationAllowed) {
            String errorMessage = "The user" + user + "is not allowed to Start the task "+task.getId();
            throw new PermissionDeniedException(errorMessage);
        }
        if (task.getTaskData().getStatus().equals(Status.Ready)) {
            
            task.getTaskData().setStatus(Status.InProgress);
            task.getTaskData().setActualOwner(user);
        }

        if (task.getTaskData().getStatus().equals(Status.Reserved)) {
            task.getTaskData().setStatus(Status.InProgress);
        }
        context.getTaskEvents().select(new AnnotationLiteral<AfterTaskStartedEvent>() {}).fire(task);

        return null;
    }

   
}
