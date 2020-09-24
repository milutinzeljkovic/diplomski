import React, { Component } from 'react';

class Records extends Component {

    renderRecords = () => {
        if(!this.props.records) return null;
        return this.props.records.map(record => {
            return (
                <div className="record-item">
                    <p>{record.record}</p>
                </div>
            )
        })
    }

    render() {
        return (
            <div className="records-container">
                {
                    this.renderRecords()
                }
            </div>
        );
    }
}

export default Records;