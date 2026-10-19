package pe.com.donpepe.minimarket.security;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.com.donpepe.minimarket.dao.UsuarioDao;
import pe.com.donpepe.minimarket.model.RolUsuario;
import pe.com.donpepe.minimarket.model.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * Servicio transversal de Seguridad de la Informacion (RF-01, RNF-03).
 * Implementa:
 * 1) Hashing criptografico con Salt aleatorio e iteraciones (PBKDF/SHA-256 + BCrypt-ready).
 * 2) Bloqueo preventivo de cuenta tras 3 intentos fallidos consecutivos.
 * 3) Sanitizacion de entradas con Apache Commons Lang3 para prevenir inyeccion.
 * 4) Enmascaramiento de datos sensibles (PII: DNI y credenciales) en bitacoras Logback.
 *
 * Autor: Equipo de Desarrollo / DevTeam
 */
public class SecurityService {
    private static final Logger log = LoggerFactory.getLogger(SecurityService.class);
    private static final int MAX_INTENTOS_FALLIDOS = 3;
    private static final int HASH_ITERATIONS = 12000;

    private final UsuarioDao usuarioDao;
    private Usuario usuarioAutenticado;

    public SecurityService(UsuarioDao usuarioDao) {
        this.usuarioDao = Preconditions.checkNotNull(usuarioDao, "El DAO de usuario no puede ser nulo");
    }

    public static String generarSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPassword(String plainPassword, String salt) {
        Preconditions.checkArgument(StringUtils.isNotBlank(plainPassword), "La contrasena no puede estar vacia");
        Preconditions.checkArgument(StringUtils.isNotBlank(salt), "El salt criptografico es obligatorio");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = (salt + ":" + plainPassword).getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < HASH_ITERATIONS; i++) {
                digest.reset();
                hash = digest.digest(hash);
            }
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Error en motor criptografico SHA-256", e);
        }
    }

    public static String maskDni(String dni) {
        if (StringUtils.isBlank(dni) || dni.length() < 4) return "********";
        return "****" + dni.substring(dni.length() - 4);
    }

    public static String sanitizeInput(String rawInput) {
        if (rawInput == null) return "";
        String cleaned = StringUtils.trimToEmpty(rawInput);
        return cleaned.replaceAll("[;'\\\"\\\\-\\\\-]", "");
    }

    public static void validarCodigoEan13(String ean13) {
        Preconditions.checkArgument(
                StringUtils.isNotBlank(ean13) && StringUtils.isNumeric(ean13) && (ean13.length() == 13 || ean13.length() == 8),
                "El codigo de barras debe ser numerico de 8 o 13 digitos (EAN-13): " + ean13
        );
    }

    public static void validarDniPeruano(String dni) {
        Preconditions.checkArgument(
                StringUtils.isNotBlank(dni) && StringUtils.isNumeric(dni) && dni.length() == 8,
                "El DNI debe contener exactamente 8 digitos numericos"
        );
    }

    public Usuario autenticar(String username, String plainPassword) {
        String cleanUser = sanitizeInput(username);
        Preconditions.checkArgument(StringUtils.isNotBlank(cleanUser), "Debe ingresar el nombre de usuario");
        Preconditions.checkArgument(StringUtils.isNotBlank(plainPassword), "Debe ingresar la contrasena");

        Optional<Usuario> opt = usuarioDao.buscarPorUsername(cleanUser);
        if (opt.isEmpty()) {
            log.warn("Intento de login con usuario inexistente: {}", cleanUser);
            throw new SecurityException("Credenciales invalidas.");
        }

        Usuario u = opt.get();
        if (u.isBloqueado() || !u.isActivo()) {
            log.error("Acceso denegado: Cuenta bloqueada por seguridad para el usuario {}", cleanUser);
            throw new SecurityException("Cuenta bloqueada tras superar 3 intentos fallidos. Contacte al Administrador.");
        }

        String computedHash = hashPassword(plainPassword, u.getSalt());
        if (!computedHash.equals(u.getPasswordHash())) {
            int nuevosIntentos = u.getIntentosFallidos() + 1;
            u.setIntentosFallidos(nuevosIntentos);
            if (nuevosIntentos >= MAX_INTENTOS_FALLIDOS) {
                u.setBloqueado(true);
                log.error("ALERTA DE SEGURIDAD: Usuario {} bloqueado tras {} intentos fallidos", cleanUser, nuevosIntentos);
            } else {
                log.warn("Contrasena incorrecta para usuario {}. Intento {}/{}", cleanUser, nuevosIntentos, MAX_INTENTOS_FALLIDOS);
            }
            usuarioDao.actualizar(u);
            throw new SecurityException("Credenciales invalidas. Intento " + nuevosIntentos + " de " + MAX_INTENTOS_FALLIDOS);
        }

        u.setIntentosFallidos(0);
        usuarioDao.actualizar(u);
        this.usuarioAutenticado = u;
        log.info("Autenticacion exitosa para usuario={} con rol={}", u.getUsername(), u.getRol());
        return u;
    }

    public boolean tienePermiso(Usuario usuario, RolUsuario... rolesPermitidos) {
        Preconditions.checkNotNull(usuario, "Sesion de usuario no iniciada");
        for (RolUsuario r : rolesPermitidos) {
            if (usuario.getRol() == r) return true;
        }
        log.warn("Acceso RBAC denegado para usuario {} con rol {}", usuario.getUsername(), usuario.getRol());
        return false;
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }
}
