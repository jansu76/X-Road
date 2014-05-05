require 'test_helper'

class SecurityServerTest < ActiveSupport::TestCase
  test "Destroy security server clients and add client deletion requests" do
    # Given
    sdsb_member_owner = get_owner()
    member_class_riigiasutus = get_riigiasutus()

    sdsb_member_client = SdsbMember.create!(
      :member_class => member_class_riigiasutus,
      :member_code => "member_client",
      :name => "Owner name",
      :administrative_contact => "a@example.com")
    
    subsystem_client = Subsystem.create!(
      :sdsb_member => sdsb_member_client,
      :subsystem_code => "subsystem_client")

    security_server_deletable = SecurityServer.create!(
      :owner => sdsb_member_owner,
      :server_code => "security_server_deletable",
      :security_server_clients => [sdsb_member_client, subsystem_client])

    # When
    SecurityServer.destroy(security_server_deletable)

    # Then
    client_deletion_requests = ClientDeletionRequest.all
    assert_equal(2, client_deletion_requests.size)

    first_request = client_deletion_requests[0]
    first_client_identifier = first_request.sec_serv_user
    assert_equal("member_client", first_client_identifier.member_code)
    assert_equal("This member should belong to group 'vallavalitsused'",
        first_request.get_server_owner_name())

    second_request = client_deletion_requests[1]
    second_client_identifier = second_request.sec_serv_user
    assert_equal("member_client", second_client_identifier.member_code)
    assert_equal("subsystem_client", second_client_identifier.subsystem_code)
    assert_equal("This member should belong to group 'vallavalitsused'",
        second_request.get_server_owner_name())
  end

  test "Should preserve owner name in request after owner deleted" do
    # Given
    owner_member = SdsbMember.create!(
      :member_class => get_riigiasutus,
      :member_code => "ownerMember",
      :name => "Owner name",
      :administrative_contact => "a@example.com")

    SecurityServer.create!(
      :owner => owner_member,
      :server_code => "security_server_deletable")

    server_id = SecurityServerId.from_parts(
      "EE", "riigiasutus", "ownerMember", "security_server_deletable")

    AuthCertDeletionRequest.new(
      :security_server => server_id,
      :auth_cert => "==bytes",
      :origin => Request::CENTER).register()

    # When
    SdsbMember.destroy(owner_member)

    # Then
    auth_cert_deletion_requests = AuthCertDeletionRequest.all
    assert_equal(1, auth_cert_deletion_requests.size)

    deletion_request = auth_cert_deletion_requests[0]
    assert_equal("Owner name", deletion_request.get_server_owner_name())
  end

  test "Should register auth cert deletion requests when destroyed" do
    # Given
    server_to_destroy = SecurityServer.create!(
      :owner => get_owner(),
      :server_code => "serverWithAuthCert")

    AuthCert.create!(
      :certificate => "--bytes",
      :security_server => server_to_destroy)

    # When
    SecurityServer.destroy(server_to_destroy)

    # Then
    auth_cert_deletion_requests = AuthCertDeletionRequest.all
    assert_equal(1, auth_cert_deletion_requests.size)

    deletion_request = auth_cert_deletion_requests[0]
    assert_equal("serverWithAuthCert",
        deletion_request.security_server.server_code)
    assert_equal("--bytes", deletion_request.auth_cert)

    assert_equal(0, AuthCert.all.size)
  end

  def get_owner
    id = ActiveRecord::Fixtures.identify(:member_in_vallavalitsused)
    SdsbMember.find(id)
  end
end