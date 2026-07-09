package de.kugi.dev.battleoftheuniverse.user;

import de.kugi.dev.battleoftheuniverse.user.dto.AdminUserView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserView toView(User user);

    UserView toView(AppUserPrincipal principal);

    AdminUserView toAdminView(User user);
}
