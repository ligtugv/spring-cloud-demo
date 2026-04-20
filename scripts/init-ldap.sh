#!/bin/bash
set -e

LDAP_HOST="${LDAP_HOST:-openldap}"
LDAP_PORT="${LDAP_PORT:-389}"
LDAP_ADMIN="cn=admin,dc=luban-cae,dc=com"
LDAP_PASS="${LDAP_ADMIN_PASSWORD:-admin_secret}"

# Wait for LDAP server
until ldapsearch -x -H "ldap://${LDAP_HOST}:${LDAP_PORT}" -b "dc=luban-cae,dc=com" -D "${LDAP_ADMIN}" -w "${LDAP_PASS}" "(objectClass=*)" 1 /dev/null 2>&1; do
    echo "[LDAP INIT] Waiting for LDAP server..."
    sleep 2
done

echo "[LDAP INIT] LDAP server ready."

# --- Users OU (idempotent: skip if exists) ---
if ! ldapsearch -x -H "ldap://${LDAP_HOST}:${LDAP_PORT}" -D "${LDAP_ADMIN}" -w "${LDAP_PASS}" "uid=ldap_user_1,ou=users,dc=luban-cae,dc=com" uid 2>/dev/null | grep -q "uid=ldap_user_1"; then
    echo "[LDAP INIT] Creating organizational units..."
    ldapadd -x -H "ldap://${LDAP_HOST}:${LDAP_PORT}" -D "${LDAP_ADMIN}" -w "${LDAP_PASS}" <<EOF
dn: ou=users,dc=luban-cae,dc=com
objectClass: organizationalUnit
ou: users

dn: ou=groups,dc=luban-cae,dc=com
objectClass: organizationalUnit
ou: groups
EOF
else
    echo "[LDAP INIT] Organizational units already exist, skipping."
fi

# --- LDAP Users (idempotent: -c continues if entry already exists) ---
echo "[LDAP INIT] Importing LDAP users..."
ldapadd -x -c -H "ldap://${LDAP_HOST}:${LDAP_PORT}" -D "${LDAP_ADMIN}" -w "${LDAP_PASS}" <<EOF
dn: uid=ldap_user_1,ou=users,dc=luban-cae,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
cn: ldap_user_1
sn: User1
uid: ldap_user_1
userPassword: ldap_user_1
uidNumber: 10001
gidNumber: 10001
homeDirectory: /home/ldap_user_1

dn: uid=ldap_editor_1,ou=users,dc=luban-cae,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
cn: ldap_editor_1
sn: Editor1
uid: ldap_editor_1
userPassword: ldap_editor_1
uidNumber: 10002
gidNumber: 10002
homeDirectory: /home/ldap_editor_1

dn: uid=ldap_adm_1,ou=users,dc=luban-cae,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
cn: ldap_adm_1
sn: Admin1
uid: ldap_adm_1
userPassword: ldap_adm_1
uidNumber: 10003
gidNumber: 10003
homeDirectory: /home/ldap_adm_1
EOF

echo "[LDAP INIT] LDAP users imported."

# --- LDAP Groups (idempotent: delete if exists, then add) ---
echo "[LDAP INIT] Importing LDAP groups..."

for group_dn in \
    "cn=PRODUCT_ADMIN,ou=groups,dc=luban-cae,dc=com" \
    "cn=EDITOR,ou=groups,dc=luban-cae,dc=com" \
    "cn=USER,ou=groups,dc=luban-cae,dc=com"; do

    if ldapsearch -x -H "ldap://${LDAP_HOST}:${LDAP_PORT}" -D "${LDAP_ADMIN}" -w "${LDAP_PASS}" "$group_dn" cn 2>/dev/null | grep -q "dn: $group_dn"; then
        echo "  Deleting existing group: $group_dn"
        ldapdelete -x -H "ldap://${LDAP_HOST}:${LDAP_PORT}" -D "${LDAP_ADMIN}" -w "${LDAP_PASS}" "$group_dn" 2>/dev/null || true
    fi
done

ldapadd -x -H "ldap://${LDAP_HOST}:${LDAP_PORT}" -D "${LDAP_ADMIN}" -w "${LDAP_PASS}" <<EOF
dn: cn=PRODUCT_ADMIN,ou=groups,dc=luban-cae,dc=com
objectClass: groupOfNames
cn: PRODUCT_ADMIN
member: uid=ldap_adm_1,ou=users,dc=luban-cae,dc=com

dn: cn=EDITOR,ou=groups,dc=luban-cae,dc=com
objectClass: groupOfNames
cn: EDITOR
member: uid=ldap_editor_1,ou=users,dc=luban-cae,dc=com

dn: cn=USER,ou=groups,dc=luban-cae,dc=com
objectClass: groupOfNames
cn: USER
member: uid=ldap_user_1,ou=users,dc=luban-cae,dc=com
member: uid=ldap_editor_1,ou=users,dc=luban-cae,dc=com
member: uid=ldap_adm_1,ou=users,dc=luban-cae,dc=com
EOF

echo "[LDAP INIT] LDAP groups imported."
echo "[LDAP INIT] Done."
