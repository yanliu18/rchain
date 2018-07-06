package coop.rchain.roscala.ob.mbox

import coop.rchain.roscala.Vm.State
import coop.rchain.roscala.ob.{Ctxt, Niv, Ob}

class EmptyMbox extends Ob {
  override def receiveMsg(client: MboxOb, task: Ctxt, state: State): Ob = {
    client.mbox = MboxOb.LockedMbox
    client.schedule(task, state)
    Niv
  }
}
