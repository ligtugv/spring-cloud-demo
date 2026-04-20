#!/bin/bash
echo "Deleting old users..."
for user in ldap_user_1 ldap_editor_1 ldap_adm_1; do
    docker exec microservice-openldap ldapdelete -x -H ldap://localhost \
        -D "cn=admin,dc=luban-cae,dc=com" -w admin_secret \
        "uid=${user},ou=users,dc=luban-cae,dc=com" 2>&1 || true
    echo "Deleted $user"
done
echo "Adding new users with correct passwords..."
docker exec microservice-openldap ldapadd -x -H ldap://localhost \
    -D "cn=admin,dc=luban-cae,dc=com" -w admin_secret <<'USERS'
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
USERS
echo "Done!"
