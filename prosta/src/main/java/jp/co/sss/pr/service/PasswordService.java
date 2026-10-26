package jp.co.sss.pr.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** パスワードのハッシュ化と照合。ハッシュ化前の平文で保存された既存データも照合できる。 */
@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        if (isHashed(storedPassword)) {
            return encoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    /** 保存値がハッシュ化されていない（旧形式）かどうか。 */
    public boolean isLegacy(String storedPassword) {
        return storedPassword != null && !isHashed(storedPassword);
    }

    private boolean isHashed(String stored) {
        return stored.startsWith("$2") && stored.length() == 60;
    }
}
