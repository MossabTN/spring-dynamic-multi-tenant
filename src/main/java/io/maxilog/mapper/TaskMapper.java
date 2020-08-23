package io.maxilog.mapper;

import io.maxilog.domain.Task;
import io.maxilog.dto.TaskDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {})
public interface TaskMapper extends EntityMapper<TaskDTO, Task> {

}
