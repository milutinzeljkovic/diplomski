import React, { Component, Fragment } from 'react';
import baseUrl from '../baseUrl';
import Records from './Records';
import Avatar from 'react-avatar';

const INITAL_STATE = {
    workers: null,
    selectedWorker: null
}

class Workers extends Component {

    constructor(){
        super();
        this.state = INITAL_STATE;
    }

    componentDidMount = async () => {
       const response = await baseUrl.get('/workers');
       this.setState({
           workers: response.data
       })
    }

    onDetails = (worker) => {
        if(worker === this.state.selectedWorker){
            this.setState({
                selectedWorker: null
            })
        }else{
            this.setState({
                selectedWorker: worker
            })
        }
    }

    renderWorkers = () => {
        if(!this.state.workers) return null;
        return this.state.workers.map(worker => {
            return (
                <Fragment>
                    <div className = 'workers-item' id={worker.id}>
                        <div className = 'workers-data'>
                            <Avatar size="40" className="users__user-content-avatar" color={Avatar.getRandomColor('sitebase', ['#e34043', '#e34043', '#e34043'])} name={worker.name} round={true} />
                            <p className='worker-name'>{worker.name}</p>
                            <p className='worker-lastname'>{worker.lastName}</p>
                        </div>
                        <button className='button' onClick={() => this.onDetails(worker)}>
                            {
                                this.state.selectedWorker === worker ?
                                "Hide records"
                                :
                                "Show records"
                            }
                        </button>
                    </div>
                    {
                        this.state.selectedWorker === worker ?
                        <Records records={worker.records}/>
                        :
                        null
                    }
                </Fragment>
            )
        })
    }

    render() {
        return (
            <div className='workers-container'>
                {
                    this.renderWorkers()
                }
            </div>
        );
    }
}

export default Workers;