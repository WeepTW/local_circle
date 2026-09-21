package com.example.localcircle.business.port;

import com.example.localcircle.business.model.Domain.*;
import java.util.List;
import java.util.Optional;

public interface RegistryRepository {
  Optional<Actor> actor(long userId);

  List<Product> products(boolean includeInactive);

  Optional<Product> product(long id, boolean lock);

  List<Account> accounts(long userId);

  Optional<Account> account(long id, long userId);

  List<Preference> preferences(long userId);

  Optional<Preference> preference(long id, long userId);

  long insert(long userId, SaveCommand command, Product product, Amounts amounts);

  int update(
      long id, long userId, long version, SaveCommand command, Product product, Amounts amounts);

  int delete(long id, long userId, long version);

  int updateProduct(long id, long userId, ProductCommand command);

  void audit(long userId, long preferenceId, String action);
}
