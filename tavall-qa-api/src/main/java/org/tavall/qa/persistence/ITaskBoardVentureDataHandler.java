package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardVenture;

import java.util.List;
import java.util.Optional;

/** Durable read boundary for venture directory records. */
public interface ITaskBoardVentureDataHandler {
    List<TaskBoardVenture> findVentures();

    Optional<TaskBoardVenture> findVenture(String slug);
}
