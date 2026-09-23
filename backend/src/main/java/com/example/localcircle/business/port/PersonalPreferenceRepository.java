package com.example.localcircle.business.port;

import com.example.localcircle.business.model.PersonalPreference.*;
import java.util.*;

public interface PersonalPreferenceRepository {
  List<View> list(long owner);

  Optional<View> get(long owner, long id);

  long insert(long owner, Values values);

  int update(long owner, long id, long version, Values values);

  long createAccount(long owner, String reference, String last4, String keyId, String payload);

  Optional<EncryptedAccount> account(long owner, long id);
}
