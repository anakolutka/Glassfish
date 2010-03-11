require 'gen/ConstGenerator'
def gen_shutdown_java(options)
  ConstGenerator.new 'platform.shutdown', options do |cg|
    cg.include "sys/socket.h"
    cg.type = :bitmask
    %w[
      SHUT_RD
      SHUT_WR
      SHUT_RDWR
    ].each {|c| cg.const c}
  end
end